package Coding

import chisel3._
import chisel3.util._
import dsptools.numbers._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.subsystem.BaseSubsystem
import org.scalacheck.Prop.False


/**
 * Base class for PacketDetect parameters
 * Type generic
 */
trait CodingParams[T <: Data] {
  val protoInOut: T
  val k: Int                        // size of smallest block of input bits
  val n: Int                        // size of smallest block of output bits
  val m: Int                        // number of memory elements. Constraint length is defined as K=m+1
  val K: Int                        // Constraint length
  val L: Int                        // length of input sequence
  val O: Int                        // OFDM bit / symbol. 48, 96, 192 can be used
  val nStates: Int                  // number of states
  val genPolynomial: List[Int]      // Matrix contains the generator polynomial
  val punctureEnable: Boolean       // enable/disable puncturing
  val punctureMatrix: List[Int]     // puncturing matrix
  val CodingScheme: Int             // 0: Convolutional Coding, 1: Turbo Coding, 2: LDPC
  val fbPolynomial: List[Int]       // feedback generator polynomial for Recursive Systematic Coding (Turbo Code)
  val tailBitingEn: Boolean         // 0: disable tail-biting, 1: enable tail-biting
  val tailBitingScheme: Int         // 0: zero tail-biting. 1: sophisticated tail-biting
}

/**
 * PacketDetect parameters for fixed-point data
 */
case class FixedCoding(
  k: Int = 1,
  n: Int = 2,
  K: Int = 3,
  L: Int = 7,
//  m: Int = 8,
//  L: Int = 6144,
  O: Int = 48,
  genPolynomial: List[Int] = List(7, 5), // generator polynomial
  punctureEnable: Boolean = false,
  punctureMatrix: List[Int] = List(6, 5), // Puncture Matrix
  CodingScheme: Int = 0,
  fbPolynomial: List[Int] = List(0),
  tailBitingEn: Boolean = false,
  tailBitingScheme: Int = 0
) extends CodingParams[UInt] {
  val protoInOut = UInt(1.W)
  val m = K - 1
  val nStates = math.pow(2.0, m.asInstanceOf[Double]).asInstanceOf[Int]
}

class CodingIO[T <: Data](params: CodingParams[T]) extends Bundle {
  val in    = Flipped(Decoupled(params.protoInOut.cloneType))
  val out   = Decoupled(Vec(params.O, params.protoInOut.cloneType))

  override def cloneType: this.type = CodingIO(params).asInstanceOf[this.type]
}
object CodingIO {
  def apply[T <: Data](params: CodingParams[T]): CodingIO[T] = new CodingIO(params)
}

class Encoding[T <: Data](params: CodingParams[T]) extends Module {
  require(params.m > 1)
  require(params.k > 0)
  require(params.n > 0)

  val io = IO(CodingIO(params))
//  val inputLength       = (params.L).asUInt(log2Ceil(params.L).W)
  // Note: m+1 memory will be instantiated because input bit will also be stored in mem(0) for simpler implementation
  val memLength         = (params.K).asUInt(log2Ceil(params.K).W)
  val shiftReg          = RegInit(Vec(Seq.fill(params.K)(0.U(1.W)))) // initialze memory with all zeros
  val termReg           = Reg(Vec(params.K, UInt(1.W)))
  val outReg            = RegInit(0.U((params.O).W))
  val regWires          = Wire(Vec(params.K, UInt(1.W)))
  val AXWires           = Wire(Vec(params.n, Vec(params.K, UInt(1.W)))) // Wires for And & Xor
  val n_cnt             = RegInit(0.U(log2Ceil(params.L).W))  // Create a counter             // may not be used
  val tail_cnt          = RegInit(0.U(log2Ceil(params.m).W))  // for zero termination         // may not be used
  val o_cnt             = RegInit(0.U(log2Ceil(params.O).W))  // counter for data vector tracker
  val p_cnt             = RegInit(0.U(log2Ceil(params.O).W))  // counter for outReg tracker
  val bufInterleaver             = RegInit(VecInit(Seq.fill(params.O)(0.U(1.W))))  // buffer for interleaver
  val puncMatBitWidth   = CodingUtils.findMinBitWidth(params.punctureMatrix)
  val outValReg         = RegInit(false.B)
  val inReadyReg        = RegInit(true.B)

  val genPolyList       = CodingUtils.dec2bitarray(params.genPolynomial, params.K)
  val genPolyVec        = Wire(Vec(params.n, Vec(params.K, UInt(1.W))))
  (0 until params.n).map(i => {
    (0 until params.K).map(j => {
      genPolyVec(i)(j)  := (genPolyList(i)(j)).U
    })
  })

  val punctureList      = CodingUtils.dec2bitarray(params.punctureMatrix, puncMatBitWidth)
  val punctureVec       = Wire(Vec(params.n, Vec(puncMatBitWidth, UInt(1.W))))
  (0 until params.n).map(i => {
    (0 until puncMatBitWidth).map(j => {
      punctureVec(i)(j) := (punctureList(i)(j)).U
    })
  })

  // puncListColSum contains summation over rows
  val puncListColSum    = punctureList.map(breeze.linalg.Vector(_)).reduce(_ + _)
  val puncPolyWire = Wire(Vec(puncMatBitWidth, UInt((log2Ceil(params.n+1)).W)))
  (0 until puncMatBitWidth).map(i => { puncPolyWire(i) := puncListColSum(i).U })

  // puncIndices contains buffer address offset
  // ex) [1,1,0],[1,0,1] -> [1,1,0],[2,1,1] : accumulate over rows
  val puncIndices = punctureList.scanLeft(Array.fill(punctureList(0).length)(0)) ((x,y) =>
    x.zip(y).map(e => e._1 + e._2)).drop(1)
  // convert this to chisel-usable variable using vector of wires
  val puncIndicesWire = Wire(Vec(params.n, Vec(puncMatBitWidth, UInt((log2Ceil(params.O)+1).W))))
  (0 until params.n).map(i => {
    (0 until puncMatBitWidth).map(j => {
      puncIndicesWire(i)(j) := puncIndices(i)(j).U
    })
  })

  // Make states for state machine
  val sStartRecv  = 0.U(2.W)        // start taking input bits
  val sEOS        = 1.U(2.W)
  val sDone       = 2.U(2.W)
  val state = RegInit(sStartRecv)

  when(io.out.fire() && state === sDone) {
    // When io.out.valid && io.out.ready both are high
    state       := sStartRecv
    o_cnt       := 0.U
    (0 until params.O).map(i => {bufInterleaver(i) := 0.U})   // zero-flush output buffer
  }

  /* hee is the logic:
  1) keep receiving input data bits
  2) start bit shifting
  3) if tail-biting is enabled, keep storing data into termReg
  3-1) zero-flush is default and 'm' 0s will be inserted in the incoming data bits
  4) when either io.in.valid or io.in.ready goes to 0, stop receiving data
  5) if tail-biting is enabled, store last 'm' bits from previous input data back to the shift registers
  */
  when(io.in.fire()) {
    shiftReg(0) := io.in.bits     // receive input from FIFO. I probably need to use MUX
    (1 to params.m).reverse.map(i => {shiftReg(i) := shiftReg(i-1) })  // start bit shifting

    // if tail-biting is selected, store last 'm' bits into termReg
    when(params.tailBitingEn.asBool() === true.B){
      (0 to params.m).map(i => { termReg(i) := shiftReg(params.m - i) })
    }

  }.otherwise {                   // below starts once we stop receiving bits from input
    when(io.in.valid === false.B) {   // io.in.valid === false.B indicate end of input sequence
      when(params.tailBitingEn.asBool() === true.B){
        (0 to params.m).map(i => { shiftReg(i) := termReg(i) })
      }
      state := sEOS
    }
  }

  // connect wires to the output of each memory element
  (0 to params.m).map(i => { regWires(i) := shiftReg(i) })

  for (i <- 0 until params.n){
    AXWires(i)(0) := regWires(0) & (genPolyVec(i)(0))                         // AND gate
    for (j <- 1 to params.m) {
      AXWires(i)(j) := AXWires(i)(j-1) ^ (regWires(j) & (genPolyVec(i)(j)))   // AND -> XOR output
    }
  }

  // check if puncturing needs to be done
  // o_cnt = data tracker
  // p_cnt = outReg index tracker
  // TODO: make below as a module


  /*
  when(pktEnd === 1){   // when packet end has been received
    put the data in output buffer with/without puncturing
    state := sDone
  }
   */

  // connect registers to output
  io.out.bits   := bufInterleaver
  io.in.ready   := state === sStartRecv   // io.out.ready is fired from FIFO sitting b/w interleaver and
  io.out.valid  := state === sDone
}
