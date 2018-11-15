package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class SRAM[T <: Data](params: CodingParams[T]) extends Module {
  val io = IO(new Bundle {
    val dataIn  = Input(Vec(params.nStates, UInt(params.m.W)))
    val dataOut = Output(Vec(params.nStates, UInt(params.m.W)))
    val push    = Input(Bool())
    val pop     = Input(Bool())
    val en      = Input(Bool())
  })
//  val survivalPath        = RegInit(VecInit(Seq.fill(params.nStates)(0.U(params.m.W))))
  val size    = params.D
  val sp      = RegInit(0.U(log2Ceil(size).W))
  val mem     = SyncReadMem(params.L * params.nStates, UInt(params.m.W))
  val dataOut = RegInit(VecInit(Seq.fill(params.nStates)(0.U(params.m.W))))
  when(io.en && io.push && (sp =/= (size-1).U)) {
    (0 until params.nStates).map(i => {
      mem.write(sp + i.U, io.dataIn(i))
    })
    sp := sp + params.nStates.U
  }.elsewhen(io.en && io.pop && (sp > 0.U)){
      sp := sp - params.nStates.U
  }

  when(io.en){
    (0 until params.nStates).map(i => {
      dataOut(i) := mem.read(sp + i.U, io.en)
    })
  }
  io.dataOut := dataOut

}
