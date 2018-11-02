package Coding

import chisel3._
import chisel3.util._
import dsptools.numbers._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.subsystem.BaseSubsystem
import org.scalacheck.Prop.False
import org.scalatest.{FlatSpec, Matchers}

class ConvCodingUnitSpec extends FlatSpec with Matchers {
  behavior of "ConvCodingUnitSpec"

  val params = FixedCoding(
    k = 1,
    n = 2,
    K = 3,
    L = 7,
    O = 6,
    genPolynomial = List(7, 5), // generator polynomial
    punctureEnable = false,
    punctureMatrix = List(6, 5), // Puncture Matrix
    CodingScheme = 0,
    fbPolynomial = List(0),
    tailBitingEn = false,
    tailBitingScheme = 0
  )
  it should "Convolution code" in {
    val inSeq   = Seq(1, 0, 1, 1, 0, 0)
    val baseTrial = ConvCodingInOut(inBit=1, stateIn = 0)
    val trials = inSeq.map { case(a) => baseTrial.copy(inBit = a)}

    FixedConvCodingTester(params, trials) should be (true)
  }
}
