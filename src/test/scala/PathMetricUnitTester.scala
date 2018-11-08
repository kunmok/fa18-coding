package Coding

import dsptools.DspTester

class PathMetricUnitTester[T <: chisel3.Data](c: PathMetric[T]) extends DspTester(c) {
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
  // below works for generator polynomial : 111, 110
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 0)
  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 0)
  expect(c.io.outPM(1), 0)
  expect(c.io.outPM(2), 0)
  expect(c.io.outPM(3), 0)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 0)
  expect(c.io.outSP(2), 0)
  expect(c.io.outSP(3), 0)

  step(1)
  poke(c.io.inReady, 1)
  poke(c.io.in(0), 0)
  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 0)
  expect(c.io.outPM(1), 0)
  expect(c.io.outPM(2), 0)
  expect(c.io.outPM(3), 0)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 0)
  expect(c.io.outSP(2), 0)
  expect(c.io.outSP(3), 0)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 1)
  poke(c.io.in(1), 1)
  expect(c.io.outPM(0), 0)
  expect(c.io.outPM(1), 100)
  expect(c.io.outPM(2), 100)
  expect(c.io.outPM(3), 100)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 0)
  expect(c.io.outSP(2), 0)
  expect(c.io.outSP(3), 0)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 1)
  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 2)
  expect(c.io.outPM(1), 100)
  expect(c.io.outPM(2), 0)
  expect(c.io.outPM(3), 101)
  expect(c.io.outSP(0), 0)
//  expect(c.io.outSP(1), 0)
  expect(c.io.outSP(2), 0)
//  expect(c.io.outSP(3), 0)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 1)
  poke(c.io.in(1), 1)
  expect(c.io.outPM(0), 3)
  expect(c.io.outPM(1), 1)
  expect(c.io.outPM(2), 3)
  expect(c.io.outPM(3), 1)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 2)
  expect(c.io.outSP(2), 0)
  expect(c.io.outSP(3), 2)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 0)
  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 2)
  expect(c.io.outPM(1), 2)
  expect(c.io.outPM(2), 2)
  expect(c.io.outPM(3), 2)
  expect(c.io.outSP(0), 1)
  expect(c.io.outSP(1), 3)
  expect(c.io.outSP(2), 1)
  expect(c.io.outSP(3), 3)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 0)
  poke(c.io.in(1), 1)
  expect(c.io.outPM(0), 2)
  expect(c.io.outPM(1), 3)
  expect(c.io.outPM(2), 3)
  expect(c.io.outPM(3), 2)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 3)
  expect(c.io.outSP(2), 1)
  expect(c.io.outSP(3), 2)

  step(1)
  poke(c.io.inReady, 0)
  poke(c.io.in(0), 1)
  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 3)
  expect(c.io.outPM(1), 2)
  expect(c.io.outPM(2), 3)
  expect(c.io.outPM(3), 4)
  expect(c.io.outSP(0), 0)
  expect(c.io.outSP(1), 3)
  expect(c.io.outSP(2), 1)
  expect(c.io.outSP(3), 3)

  step(1)
  poke(c.io.inReady, 0)
//  poke(c.io.in(0), 1)
//  poke(c.io.in(1), 0)
  expect(c.io.outPM(0), 2)
  expect(c.io.outPM(1), 4)
  expect(c.io.outPM(2), 4)
  expect(c.io.outPM(3), 4)
  expect(c.io.outSP(0), 1)
  expect(c.io.outSP(1), 2)
  expect(c.io.outSP(2), 1)
  expect(c.io.outSP(3), 3)

}

  /**
    * Convenience function for running tests
    */
object FixedPathMetricTester {
  def apply(params: FixedCoding): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new PathMetric(params)) {
      c => new PathMetricUnitTester(c)
    }
  }
}
