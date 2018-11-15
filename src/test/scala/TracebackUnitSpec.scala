package Coding

import org.scalatest.{FlatSpec, Matchers}

class TracebackUnitSpec extends FlatSpec with Matchers {
  behavior of "TracebackUnitSpec"

  val params = FixedCoding(
    k = 1,
    n = 2,
    K = 3,
    L = 2,
    O = 6,
    D = 5,
    genPolynomial = List(7, 5), // generator polynomial
    punctureEnable = true,
    punctureMatrix = List(6, 5), // Puncture Matrix
    CodingScheme = 0,
    fbPolynomial = List(0),
    tailBitingEn = false,
    tailBitingScheme = 0
  )
  it should "Traceback" in {

    FixedTracebackTester(params) should be (true)
  }
}
