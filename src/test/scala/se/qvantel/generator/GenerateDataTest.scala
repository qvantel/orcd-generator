package se.qvantel.generator

import org.scalatest.FunSuite

class GenerateDataTest extends FunSuite {

  test("Destination is generated") {
    assert(GenerateData.destination().contains("000FFFFFF"))
  }

  test("Msisdn is generated and has length of 10 characters") {
    assert(GenerateData.msisdn().length==10)
  }

  test("Msisdn is randomized") {
    val r1 = GenerateData.msisdn()
    val r2 = GenerateData.msisdn()
    assert(r1 != r2)
  }
}
