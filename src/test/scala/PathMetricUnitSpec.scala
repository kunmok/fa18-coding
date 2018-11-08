package Coding

import org.scalatest.{FlatSpec, Matchers}

class PathMetricUnitSpec extends FlatSpec with Matchers {
  behavior of "Path Metric UnitSpec"

  it should "Calculate Path Metric" in {

    val params = FixedCoding(
      k = 1,
      n = 2,
      K = 3,
      L = 100,
      O = 6,
      genPolynomial = List(7, 6), // generator polynomial
      punctureEnable = true,
      punctureMatrix = List(6, 5), // Puncture Matrix
      CodingScheme = 0,
      fbPolynomial = List(0),
      tailBitingEn = false,
      tailBitingScheme = 0
    )

    FixedPathMetricTester(params) should be (true)
  }
}
