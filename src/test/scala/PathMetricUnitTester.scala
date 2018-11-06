package Coding

import dsptools.DspTester

case class PathMetricInOut(
  // input sequence
  inBit: Int,
  stateIn: Int,
  outBitSeq: Option[List[Int]] = None,

)

class PathMetricUnitTester[T <: chisel3.Data](c: PathMetric[T], trials: Seq[PathMetricInOut]) extends DspTester(c) {
  /*
Following is for G=(7, 5)
State | In  | Out | Next State
00    | 0   | 00  | 00
00    | 1   | 11  | 10

10    | 0   | 10  | 01
10    | 1   | 01  | 11

01    | 0   | 11  | 00
01    | 1   | 00  | 10

11    | 0   | 01  | 01
11    | 1   | 10  | 11
 */
//  poke(c.io.in(0), 0)
//  poke(c.io.in(1), 0)
//  expect(c.io.out(0), 0)
//  expect(c.io.out(1), 0)
//  expect(c.io.out(2), 0)
//  expect(c.io.out(3), 0)
}

  /**
    * Convenience function for running tests
    */
object FixedPathMetricTester {
  def apply(params: FixedCoding, trials: Seq[PathMetricInOut]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new PathMetric(params)) {
      c => new PathMetricUnitTester(c, trials)
    }
  }
}
