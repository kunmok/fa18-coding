package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class PathMetric[T <: Data](params: CodingParams[T]) extends Module {
  require(params.m > 1)
  require(params.k > 0)
  require(params.n > 0)

  val io = IO(new Bundle {
    val in        = Input(Vec(params.n, UInt(1.W)))
    val out       = Output(Vec(params.nStates, UInt(16.W)))
  })
  val branchMetricModule  = Module(new BranchMetric[T](params))
  val trellisObj          = new Trellis[T](params)
  val nextStateTable      = trellisObj.nextstate_table


  val numRows             = math.pow(2.0, (params.m-1).asInstanceOf[Double]).asInstanceOf[Int]
  val survivalPath        = Reg(Vec(params.nStates, UInt((log2Ceil(params.n)+1).W)))
  val tmpSP               = Wire(Vec(params.nStates, Vec(params.numInputs, UInt((log2Ceil(params.n)+1).W))))
  for (currentInput <- 0 until params.numInputs){
    for (currentStates <- 0 until params.nStates){
      tmpSP(currentStates/2+currentInput*numRows)(currentStates%2) := currentStates.U
    }
  }

  val pmRegs              = RegInit(VecInit(Seq.fill(params.nStates)(0.U(params.pmBits.W))))
//  (1 until params.nStates).map(i => {pmRegs(i) := 10000.U})

  // temporary matrix for Path Metric calculation
  // TODO: How to find the maximum # of bits for PM ?
  val tmpPM               = Wire(Vec(params.nStates, Vec(params.numInputs, UInt(params.pmBits.W))))
  for (currentInput <- 0 until params.numInputs){
    for (currentStates <- 0 until params.nStates){
      tmpSP(currentStates/2+currentInput*numRows)(currentStates%2) := pmRegs(currentStates)
    }
  }

  // temporary matrix for Branch Metric calculation
  val tmpBM               = Wire(Vec(params.nStates, Vec(params.numInputs, UInt((log2Ceil(params.n)+1).W))))
  for (currentInput <- 0 until params.numInputs){
    for (currentStates <- 0 until params.nStates){
      // TODO:
    }
  }

//  (0 until params.n).map(i => {branchMetricModule.io.in(i) := io.in(i)})

//  pmRegs := tmpPM_prime
//  io.out := tmpPM_prime
}
