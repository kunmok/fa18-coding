package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class BranchMetric[T <: Data](params: CodingParams[T]) extends Module {
  require(params.m > 1)
  require(params.k > 0)
  require(params.n > 0)

  val io = IO(new Bundle {
    val in        = Input(Vec(params.n, UInt(1.W)))
    val out       = Output(Vec(params.nStates, Vec(params.numInputs, Vec(params.n, UInt(1.W)))))
  })

//  val bmWires     = Wire(Vec(params.nStates, Vec(params.numInputs, Vec(params.n, UInt(1.W)))))
  val trellisObj  = new Trellis[T](params)

  for (currentStates <- 0 until params.nStates) {
    for (currentInputs <- 0 until params.numInputs) {
      for (r <- 0 until params.n) {
        io.out(currentStates)(currentInputs)(r) := io.in(r) ^ trellisObj.output_table(currentStates)(currentInputs)(r).U
//        io.out(currentStates)(currentInputs)(r) := io.in(r) ^ 1.U
      }
    }
  }

//  io.out := bmWires
}
