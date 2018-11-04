package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem


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
//  val out   = Decoupled(Vec(params.n, params.protoInOut.cloneType))

  override def cloneType: this.type = CodingIO(params).asInstanceOf[this.type]
}
object CodingIO {
  def apply[T <: Data](params: CodingParams[T]): CodingIO[T] = new CodingIO(params)
}

class Encoding[T <: Data](params: CodingParams[T]) extends Module {
  require(params.m > 1)
  require(params.k > 0)
  require(params.n > 0)

  // Make states for state machine
  val sStartRecv  = 0.U(2.W)        // start taking input bits
  val sEOS        = 1.U(2.W)
  val sDone       = 2.U(2.W)
  val state = RegInit(sStartRecv)

  val io = IO(CodingIO(params))
  val convCodingModule  = Module(new ConvCoding[T](params))
  val puncturingModule  = Module(new Puncturing[T](params))

  when(io.in.fire() && state === sStartRecv){
    convCodingModule.io.inReady := 1.U
  }.otherwise{
    convCodingModule.io.inReady := 0.U
  }

  convCodingModule.io.in := io.in.bits
  convCodingModule.io.stateIn := state

  puncturingModule.io.in := convCodingModule.io.out
  puncturingModule.io.stateIn := convCodingModule.io.stateOut
  puncturingModule.io.inReady := convCodingModule.io.outReady

  when(io.out.fire() && puncturingModule.io.stateOut === sDone) {
    state       := sStartRecv
  }
//  state := convCodingModule.io.stateOut

  // connect registers to output
  io.out.bits   := puncturingModule.io.out
//  io.out.bits   := convCodingModule.io.out
  io.in.ready   := state === sStartRecv   // io.out.ready is fired from FIFO sitting b/w interleaver and
  io.out.valid  := state === sDone
}
