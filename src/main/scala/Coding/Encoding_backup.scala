//package modem
//
//import chisel3._
//import chisel3.experimental.FixedPoint
//import chisel3.util._
//import dsptools.numbers._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem
//import modem.Utils
//import org.scalacheck.Prop.False
//
//
///**
// * Base class for PacketDetect parameters
// * Type generic
// */
//trait CodingParams[T <: Data] {
//  val protoInOut: T
//  val k: Int                        // size of smallest block of input bits
//  val n: Int                        // size of smallest block of output bits
//  val m: Int                        // number of memory elements. Constraint length is defined as K=m+1
//  val K: Int                        // Constraint length
//  val L: Int                        // length of input sequence
//  val O: Int                        // OFDM bit / symbol. 48, 96, 192 can be used
//  val nStates: Int                  // number of states
//  val genPolynomial: List[Int]      // Matrix contains the generator polynomial
//  val punctureEnable: Boolean       // enable/disable puncturing
//  val punctureMatrix: List[Int]     // puncturing matrix
//  val CodingScheme: Int             // 0: Convolutional Coding, 1: Turbo Coding, 2: LDPC
//  val fbPolynomial: List[Int]       // feedback generator polynomial for Recursive Systematic Coding (Turbo Code)
//  val tailBitingEn: Boolean         // 0: disable tail-biting, 1: enable tail-biting
//  val tailBitingScheme: Int         // 0: zero tail-biting. 1: sophisticated tail-biting
//}
//
///**
// * PacketDetect parameters for fixed-point data
// */
//case class FixedCoding(
//  k: Int = 1,
//  n: Int = 2,
//  m: Int = 3,
//  L: Int = 7,
////  m: Int = 8,
////  L: Int = 6144,
//  O: Int = 48,
//  genPolynomial: List[Int] = List(7, 5), // generator polynomial
//  punctureEnable: Boolean = false,
//  punctureMatrix: List[Int] = List(6, 5), // Puncture Matrix
//  CodingScheme: Int = 0,
//  fbPolynomial: List[Int] = List(0),
//  tailBitingEn: Boolean = false,
//  tailBitingScheme: Int = 0
//) extends CodingParams[UInt] {
//  val protoInOut = UInt(1.W)
//  val K = m + 1
//  val nStates = math.pow(2.0, m.asInstanceOf[Double]).asInstanceOf[Int]
//}
//
//class CodingIO[T <: Data](params: CodingParams[T]) extends Bundle {
//  val in    = Flipped(Decoupled(params.protoInOut.cloneType))
//  val out   = Decoupled(Vec(params.O, params.protoInOut.cloneType))
//
//  override def cloneType: this.type = CodingIO(params).asInstanceOf[this.type]
//}
//object CodingIO {
//  def apply[T <: Data](params: CodingParams[T]): CodingIO[T] = new CodingIO(params)
//}
//
//class Encoding[T <: Data](params: CodingParams[T]) extends Module {
//  require(params.m > 1)
//  require(params.k > 0)
//  require(params.n > 0)
//
//  val io = IO(CodingIO(params))
//  val inputLength       = (params.L).asUInt(log2Ceil(params.L).W)
//  // Note: m+1 memory will be instantiated because input bit will also be stored in mem(0) for simpler implementation
//  val memLength         = (params.K).asUInt(log2Ceil(params.K).W)
//  val shiftReg          = RegInit(Vec(Seq.fill(params.K)(0.U(1.W)))) // initialze memory with all zeros
//  //val shiftRegV2        = Module(new ShiftReg(io.in.bits, params.K))
//  val termReg           = Reg(Vec(params.K, UInt(1.W)))
//  val outReg            = RegInit(0.U((params.O).W))
//  val regWires          = Wire(Vec(params.K, UInt(1.W)))
//  val AXWires           = Wire(Vec(params.n, Vec(params.K, UInt(1.W)))) // Wires for And & Xor
//  val n_cnt             = RegInit(0.U(log2Ceil(params.L).W))  // Create a counter
//  val o_cnt             = RegInit(0.U(log2Ceil(params.O).W))  // counter for output vector
//  val tail_cnt          = RegInit(0.U(log2Ceil(params.m).W))  // for zero termination
//  val deser             = RegInit(Vec(Seq.fill(params.O)(0.U(1.W))))
//  val puncMatBitWidth   = Utils.findMinBitWidth(params.punctureMatrix)
//  val outValReg         = RegInit(false.B)
//
//  val genPolyList       = Utils.dec2bitarray(params.genPolynomial, params.K)
//  val genPolyVec        = Wire(Vec(params.n, Vec(params.K, UInt(1.W))))
//  for (x <- 0 until params.n)
//    for (y <- 0 until params.K)
//      genPolyVec(x)(y) := (genPolyList(x)(y)).U
//
//  //  (0 until params.n).map(x => {
//  //    (0 until params.K).map(y => {
//  //      genPolyVec(x)(y)  := (genPolyList(x)(y)).U
//  //    })
//  //  })
//
//  val genPolyList = Utils.dec2bitarray(params.genPolynomial, params.K)
//  val puntureList = Utils.dec2bitarray(params.punctureMatrix, puncMatBitWidth)
//  val punctureVec       = Wire(Vec(params.n, Vec(puncMatBitWidth, UInt(1.W))))
//  (0 until params.n).map(x => {
//    (0 until puncMatBitWidth).map(y => {
//      punctureVec(x)(y) := (puntureList(x)(y)).U
//    })
//  })
//
//  // Make states for state machine
//  val sStartRecv  = 0.U(3.W)        // start taking input bits
//  val sTailBiting = 1.U(3.W)        // stop receiving data & start tail-biting
//  val sBreak      = 2.U(3.W)        // Stop receiving data & stop tail-biting
////  val sWork = 2.U(3.W)
////  val sDone = 3.U(3.W)
//  val state = RegInit(sStartRecv)
//
//  /* hee is the logic:
//  1) check if input bit has been received 'L' times
//  1-1) initially we are in state 'sStartRecv'
//  2) if the input has not been received 'L' times YET
//  2-1) shiftReg(0) takes value from io.in.bits
//  2-2) perform bit shifting
//  2-3) increase counter value
//  3) if input has been received 'L' times, preparing for tail-biting
//  3-1) store current values in each register in "termReg"
//  3-2) change state from sStartRecv to sTailBiting -> this will force 'io.in.ready := false.B'
//  3-3) reset count to 0
//  4) Once we stop receiving data, perform tail-biting
//  5) check if advanced tail-biting scheme is preferred
//  5-1) if so, then put termReg back to the registers and hold this value (do not perform bit shifting)
//  5-2) if zero tail-biting is preferred, put 0 into shiftreg(0) and start shifting 'm' times.
//       this will fill up all the 'm' registers with '0' after 'm' clock cycles
//  5-3) if 'm' clock cycle has been passed, then move to other state called 'sBreak' and reset counter for tail-biting
//  6) go back to original state 'sStartRecv'
//  */
//
//  when(io.in.fire()) {
//    // check if input has been received 'L' times
//    when(n_cnt < inputLength){
//      shiftReg(0) := io.in.bits                         // receive input from FIFO. I probably need to use MUX
//      for (i <- (1 to params.m).reverse) {              // start bit shifting
//        shiftReg(i) := shiftReg(i-1)
//      }
//      n_cnt := n_cnt + 1          // increase counter value
//    }.otherwise{
//      when(params.tailBitingScheme.asUInt(1.W) === 1.U) {
//        // if tail-biting is selected, store last 'm' bits into termReg
//        for (i <- 0 to params.m) {
//          termReg(i) := shiftReg(params.m - i)
//        }
//      }
//      when(params.tailBitingEn.asBool() === true.B) {
//        state := sTailBiting      // start tail-biting when it receives all the input sequences
//      }
//      n_cnt := 0.U                // reset the n_cnt and lower the "ready" flag
//    }
//  }.otherwise {                   // below starts once we stop receiving bits from input
//    when(state === sTailBiting ) {
//      when(params.tailBitingScheme.asUInt(1.W) === 1.U) { // advanced tail-biting starts
//        for (i <- 0 to params.m) {
//          shiftReg(i) := termReg(i)
//        }
//        state := sStartRecv         // repeat data receiving
//      }.otherwise { // zero tail-biting starts
//        when(tail_cnt < memLength) {
//          shiftReg(0) := false.B // feed '0' to the first memory element
//          for (i <- (1 to params.m).reverse) {
//            shiftReg(i) := shiftReg(i - 1) // shift bits
//          }
//          tail_cnt := tail_cnt + 1
//          // below is using higher-order functions
//          // shiftReg(0) := false.B
//          //          shiftReg.foldLeft(false.B){
//          //            case (reg, inp) => {
//          //              reg := inp
//          //              reg
//          //            }
//          //          }
//          // tail_cnt := tail_cnt + 1
//        }.otherwise {
//          // finish termination and move on to the next state when tail_cnt reaches to its max
//          tail_cnt := 0.U // reset the counter and stop feeding any data to the first memory element
//          state := sStartRecv // tail-biting has been completed. we go back and start receiving data
//        }
//      }
//    }
//    // TODO: if (io.in.ready = false & state = sStartRecv), what should I do here?
//    // TODO: suggestion 1) do nothing.
//    // TODO: there might be a case that L is not set and MAC instead signals io.in.valid as an indication of EOS
//    // TODO:
//  }
//
//  // connect wires to the output of each memory element
//  for (i <- 0 to params.m) {
//    regWires(i) := shiftReg(i)
//  }
//
//  // Generate encoded data
////  AXWires.zip(genPolyList).foreach{
////    case (a, b) => {
////      a(0) := regWires(0) & b(0).U(1.W)
////      (1 to params.m).map(
////        m => a(m) := a(m-1) ^ (regWires(m) & (b(m).U(1.W)))
////      )
////    }
////  }
//
//  for (i <- 0 until params.n){
//    AXWires(i)(0) := regWires(0) & (genPolyList(i)(0)).U(1.W)                         // AND gate
//    for (j <- 1 to params.m) {
//      AXWires(i)(j) := AXWires(i)(j-1) ^ (regWires(j) & (genPolyList(i)(j)).U(1.W))   // AND -> XOR output
//    }
//  }
//
//  // check if puncturing needs to be done
//  val tmp: UInt = (o_cnt / params.n.U) % (params.n.U * puncMatBitWidth.U)
//  when(o_cnt < (params.O).U){
//    when(params.punctureEnable.B === true.B) {
//      when(punctureVec(o_cnt % params.n.U)(tmp) === 1.U){
//        deser(o_cnt) := AXWires(o_cnt % params.n.U)(params.m.U)
//        o_cnt := o_cnt + (params.n).U
//      }
//    }.otherwise {   // puncturing is disabled, put the data directly into the buffer
//      for (i <- 0 until params.n){
//        deser(o_cnt + i.U) := AXWires((o_cnt + i.U) % params.n.U)(params.m.U)
//        o_cnt := o_cnt + (params.n).U
//      }
//    }
//  }.otherwise{
//    io.out.bits := deser
//    outValReg := true.B
//    o_cnt := 0.U
//  }
//
//  when(io.out.fire() && outValReg === true.B) {
//    // When io.out.valid && io.out.ready both are high
//    outValReg := false.B
//  }
//
//  // connect registers to output
//  io.out.bits   := deser
//  io.in.ready   := (state === sStartRecv) || (io.out.ready)   // io.out.ready is fired from FIFO sitting b/w interleaver and
//  io.out.valid  := outValReg
//}
//
///*
//  TODO: this code will be used if we decide to remove 'L' from the list of inputs
//  TODO: below assumes that MAC layer does zero termiation for encoder
//  TODO: tailBitingEn -> it means zero-flushing is not being used
//  when(io.in.fire()) {
//    shiftReg(0) := io.in.bits                         // receive input from FIFO. I probably need to use MUX
//    for (i <- (1 to params.m).reverse)                // start bit shifting
//      shiftReg(i) := shiftReg(i-1)
//
//    when(params.tailBitingEn.asBool() === true.B) {
//      // if tail-biting is selected, store last 'm' bits into termReg
//      for (i <- 0 to params.m)
//        termReg(i) := shiftReg(params.m - i)
//    }
//  }.otherwise {                   // below starts once we stop receiving bits from input
//    when(params.tailBitingEn.asBool() === true.B) {
//      for (i <- 0 to params.m)
//        shiftReg(i) := termReg(i)
//    }
//  }
//*/