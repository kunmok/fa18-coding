package Coding

import chisel3._
import dsptools.DspTester
import org.scalatest.{FlatSpec, Matchers}

case class ConvCodingInOut(
  // input sequence
  inBit: Int,
  stateIn: Int,
  // optional outputs
  // if None, then don't check the result
  // if Some(...), check that the result matches
  outBitSeq: Option[List[Int]] = None,

)

class ConvCodingUnitTester[T <: chisel3.Data](c: ConvCoding[T], trials: Seq[ConvCodingInOut]) extends DspTester(c) {
  poke(c.io.in, 1)
  poke(c.io.stateIn, 0)
  expect(c.io.out(0), 0)
  expect(c.io.out(1), 0)
}

  /**
    * Convenience function for running tests
    */
object FixedConvCodingTester {
  def apply(params: FixedCoding, trials: Seq[ConvCodingInOut]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new ConvCoding(params)) {
      c => new ConvCodingUnitTester(c, trials)
    }
  }
}
