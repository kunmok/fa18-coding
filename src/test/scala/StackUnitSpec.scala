package Coding

import org.scalatest.{FlatSpec, Matchers}

class StackUnitSpec extends FlatSpec with Matchers {
  behavior of "Stack Memory UnitSpec"

  it should "stack data in memory" in {

    val params = FixedCoding(
      k = 1,
      n = 2,
      K = 3,
      L = 100,
      O = 6,
      D = 36,
      genPolynomial = List(7, 6), // generator polynomial
      punctureEnable = true,
      punctureMatrix = List(6, 5), // Puncture Matrix
      CodingScheme = 0,
      fbPolynomial = List(0),
      tailBitingEn = false,
      tailBitingScheme = 0
    )

    FixedStackTester(params, params.L) should be (true)
  }
}
