package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class Stack[T <: Data](params: CodingParams[T], size: Int) extends Module {
  val io = IO(new Bundle {
    val dataIn  = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val push    = Input(Bool())
    val pop     = Input(Bool())
    val en      = Input(Bool())
  })

  // declare the memory for the stack
  val stack_mem = Mem(size, UInt(32.W))
  val sp = RegInit(0.U(log2Ceil(size).W))
  val dataOut = RegInit(0.U(32.W))

  // Push condition - make sure stack isn't full
  when(io.en && io.push && (sp =/= (size-1).U)) {
    stack_mem(sp + 1.U) := io.dataIn
    sp := sp + 1.U
  }
    // Pop condition - make sure the stack isn't empty
    .elsewhen(io.en && io.pop && (sp > 0.U)) {
    sp := sp - 1.U
  }

  when(io.en) {
    dataOut := stack_mem(sp)
  }

  io.dataOut := dataOut
}
