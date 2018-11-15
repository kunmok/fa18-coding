package Coding

import org.scalatest.{FlatSpec, Matchers}

class DePuncturingUnitSpec extends FlatSpec with Matchers {
  behavior of "DePuncturingUnitSpec"

  val params = FixedCoding(
    k = 1,
    n = 2,
    K = 3,
    L = 7,
    O = 10,
    D = 36,
    genPolynomial = List(7, 5), // generator polynomial
    punctureEnable = true,
    punctureMatrix = List(6, 5), // Puncture Matrix
    CodingScheme = 0,
    fbPolynomial = List(0),
    tailBitingEn = false,
    tailBitingScheme = 0
  )
  it should "de-puncturing code" in {
    FixedDePuncturingTester(params) should be (true)
  }
}
