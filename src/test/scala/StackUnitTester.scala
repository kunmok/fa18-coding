package Coding

import chisel3._
import chisel3.util._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import org.scalacheck.Prop.False

class StackUnitTester[T <: chisel3.Data](c: Stack[T]) extends PeekPokeTester(c) {
  poke(c.io.en, 1)
  poke(c.io.pop, false)
  poke(c.io.pop, false)
  step(1)
  poke(c.io.dataIn, 3)
  poke(c.io.push, true)
  step(1)
  poke(c.io.push, false)
  poke(c.io.pop, true)
  step(1)
  poke(c.io.dataIn, 4)
  poke(c.io.push, true)
  poke(c.io.pop, false)
  expect(c.io.dataOut, 3)   // nothing on stack at this point
  step(1)
  poke(c.io.dataIn, 5)
  step(1)
  poke(c.io.dataIn, 6)
  poke(c.io.pop, false)
  step(1)
  poke(c.io.push, false)
  poke(c.io.pop, true)
  step(1)
  expect(c.io.dataOut, 6)
  step(1)
  expect(c.io.dataOut, 5)
  step(1)
  expect(c.io.dataOut, 4)
  poke(c.io.pop, false)
}

object FixedStackTester {
  def apply(params: FixedCoding, stack_size: Int): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new Stack(params, stack_size)) {
      c => new StackUnitTester(c)
    }
  }
}
