package Coding

import breeze.numerics.pow
import chisel3._
import chisel3.util._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

class Trellis[T <: Data](params: CodingParams[T]) extends Module {
  require(params.m > 1)
  require(params.k > 0)
  require(params.n > 0)

//  val io = IO(new Bundle {
//    val in        = Input(UInt(1.W))    // assuming k=1 for all convolutional coding
//    val out       = Output(Vec(params.n, UInt(1.W)))
//
//    val inReady   = Input(UInt(1.W))
//    val outReady  = Output(UInt(1.W))
//
//    val stateIn   = Input(UInt(2.W))
//    val stateOut  = Output(UInt(2.W))
//  })
  val numInputs   = math.pow(2.0, params.k.asInstanceOf[Double]).asInstanceOf[Int]
  val output_table = Array.ofDim[Int](params.nStates, numInputs, params.n)       // array storing outputs
  val nextstate_table = Array.ofDim[Int](params.nStates, numInputs, params.m)    // array storing next states
  val outbits = Array.fill(params.n){0}
  val shiftReg = Array.fill(params.m){0}
  val generatorArray = Array.fill(params.K){0}
    CodingUtils.dec2bitarray_unit(params.genPolynomial(r), params.K)
  for (currentStates <- 0 until params.nStates){
    for (currentInputs <- 0 until numInputs){
      (0 until params.n).map(i => {outbits(i) = 0 })            // reset outbits to zeros
      for (r <- 0 until params.n){
        (0 until params.m).map(i => {shiftReg(i) = CodingUtils.dec2bitarray_unit(currentStates, params.m)(i) })
        (0 until params.K).map(i => {generatorArray(i) = CodingUtils.dec2bitarray_unit(params.genPolynomial(r), params.K)(i)})
// check point
        for (i <- 0 until params.m){
          outbits(r) = (outbits(r) + shiftReg(i+1)*generatorArray(r)(i+1)) % 2
        }
        val output_generator_array = generatorArray(r)(0)
        (1 to params.m).reverse.map(i => {shiftReg(i) = shiftReg(i-1) })  // start bit shifting
        shiftReg(0) = currentInputs % 2

        outbits(r) = (outbits(r) + (currentInputs * output_generator_array) % 2) % 2
      }
      output_table(currentStates)(currentInputs) = outbits
      nextstate_table(currentStates)(currentInputs) = shiftReg
    }
  }
}
