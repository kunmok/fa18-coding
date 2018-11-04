package Coding

/**
 * Object for computing useful constants
 */
object CodingUtils {

  def dec2bitarray_unit(in_number: Int, bit_width: Int) = {
    val bitarray = Array.fill(bit_width){0}
    val binary_string = in_number.toBinaryString
    if(bit_width <= binary_string.length) {
      for (j <- 0 until bit_width) {
        bitarray(bit_width - j - 1) = binary_string(bit_width - j - 1).asDigit
      }
    }else{
      for (j <- 0 until binary_string.length){
        bitarray(bit_width-j-1) = binary_string(binary_string.length-j-1).asDigit
      }
    }
    bitarray
  }

  def dec2bitarray(genPolynoimial: List[Int], bit_width: Int) = {
    val numRow = genPolynoimial.length
    val bitarray = Array.ofDim[Int](numRow, bit_width)
    for (i <- 0 until numRow){
      bitarray(i) = dec2bitarray_unit(genPolynoimial(i), bit_width)
    }
    bitarray
  }

  def findMinBitWidth(arg: List[Int]): Int = {
    val result: Int = math.ceil(math.log10(arg.max)/math.log10(2)).asInstanceOf[Int]
    result
  }

//  def onesInPuncMat(arg: Array[Array[Int]]): Unit ={
//    arg.map(breeze.linalg.Vector(_)).reduce(_ + _)
//  }
}
