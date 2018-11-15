package Coding

import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class Traceback[T <: Data](params: CodingParams[T]) extends Module {
  val io = IO(new Bundle {
    val inPM    = Input(Vec(params.nStates, UInt(params.pmBits.W))) // storing Path Metric
    val inSP    = Input(Vec(params.nStates, UInt(params.m.W))) // storing Survival Path
    val inReady = Input(UInt(1.W))
    val out     = Decoupled(Vec(params.D, UInt(params.k.W)))

  })
  val L   = params.L
  val D   = params.D
  val m   = params.m

  // Memory Configuration
  val outValid    = RegInit(false.B)
  val addrSize    = params.nStates * (D+L)
  val addrWidth   = log2Ceil(addrSize) + 2
  val addr        = Wire(UInt(addrWidth.W))
  val enable      = Wire(Bool())
  val addrReg     = RegInit(0.U(addrWidth.W))
  val tmpSP       = Wire(Vec(D+L, UInt(m.W)))
  val rotateReg   = RegInit(0.U(1.W)) // indicating which half of memory is currently being used

  // setup registers for address
  addr    := addrReg
  enable  := true.B // TODO: connect this to the input of the module
  when(addrReg < (addrSize * 2).U){
    addrReg := addrReg + params.nStates.U
  }.otherwise{
    addrReg := 0.U
  }

  // Store data into memory
  val mem = SyncReadMem(addrSize * 2, UInt(m.W))
  for (i <- 0 until params.nStates) {
    mem.write(addrReg + i.U, io.inSP(i))
  }

  // declare variables for decoding process
  val decodeReg         = Reg(Vec(D, UInt(params.k.W)))
  val tmpSPReg          = RegInit(VecInit(Seq.fill(params.nStates)(0.U(m.W))))
  val tmpPMMin          = Wire(Vec(params.nStates - 1, UInt(m.W)))
  val tmpPMMinIndex     = Wire(Vec(params.nStates - 1, UInt(m.W)))
  val tmpPMMinReg       = RegInit(VecInit(Seq.fill(params.nStates - 1)(0.U(m.W))))
  val tmpPMMinIndexReg  = RegInit(VecInit(Seq.fill(params.nStates - 1)(0.U(m.W))))

  // find minimum in PM
  tmpPMMin(0)           := Mux(io.inPM(0) < io.inPM(1), io.inPM(0), io.inPM(1))
  tmpPMMinIndex(0)      := Mux(io.inPM(0) < io.inPM(1), 0.U, 1.U)
  for (i <- 1 until params.nStates - 1) {
    tmpPMMin(i)         := Mux(tmpPMMin(i - 1) < io.inPM(i + 1), tmpPMMin(i - 1), io.inPM(i + 1))
    tmpPMMinIndex(i)    := Mux(tmpPMMin(i - 1) < io.inPM(i + 1), tmpPMMinIndex(i - 1), (i + 1).U)
  }
  // should be clocked at nState * (D+L-1)
  when(addrReg % (params.nStates * (D+L)).U === (params.nStates * (D+L-1)).U) {
    tmpPMMinReg         := tmpPMMin
    tmpPMMinIndexReg    := tmpPMMinIndex
    tmpSPReg            := io.inSP
  }

  // Start decoding
  /*  example: D = 5, D = traceback depth of Viterbi decoder
      nState * (D + L - 1)  -> data have been received 'D' times
      nState * (D + L)      -> 'D' data is stored in memory
      nState * (D + L + 1)  -> 'mem.read' is called. Fetch the data from the memory.
      nState * (D + L + 2)  -> data should be fetched on 'decodeReg'. raise 'valid'
      nState * (D + L + 3)  -> fetch data from 'decodeReg' to 'io.out.bits'
  */
  tmpSP(D+L-1) := tmpSPReg(tmpPMMinIndexReg(params.nStates-2))
  for (i <- D+L-2 to 0 by -1) {
    tmpSP(i) := mem.read((params.nStates * i).U + tmpSP(i + 1))
    if(i < D) {
      decodeReg(i) := tmpSP(i+1) >> (m-1) // get MSB
    }
  }

  when(addrReg === (params.nStates * (D+L+2)).U){
    outValid      := true.B
  }
  when(io.out.fire()){
    outValid      := false.B      // receiver successfully received the data.
  }
  io.out.valid    := outValid
  io.out.bits     := decodeReg    // output is available 3 clk cycles after.
}


//  val readEn  = addrReg === (params.nStates * (D+1)).U
//  val regEn   = addrReg === (params.nStates * (D+2)).U
//  tmpSP(D-1)  := tmpSPReg(tmpPMMinIndexReg(params.nStates-2))
//  when (regEn) {
//    decodeReg(D-1)  := tmpPMMinIndexReg(params.nStates-2) >> (m-1)
//  }
//  for (i <- D-2 to 0 by -1) {
//    tmpSP(i)        := mem.read((params.nStates * i).U + tmpSP(i + 1), readEn)
//    when (regEn) {
//      decodeReg(i)  := tmpSP(i+1) >> (m-1) // get MSB
//    }
//  }