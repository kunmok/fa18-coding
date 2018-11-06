package Coding

import chisel3._
import chisel3.util._

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
  val numInputs: Int
  val pmBits: Int
}

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
  val numInputs   = math.pow(2.0, k.asInstanceOf[Double]).asInstanceOf[Int]
  val pmBits = log2Ceil(L * (math.pow(2.0, n.asInstanceOf[Double]).asInstanceOf[Int] - 1))
}