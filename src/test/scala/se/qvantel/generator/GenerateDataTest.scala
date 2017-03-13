package se.qvantel.generator

import org.scalatest.FunSuite
import se.qvantel.generator.utils.property.DataConfig

class GenerateDataTest extends FunSuite with DataConfig {

  test("Product is generated") {
    assert(GenerateData.product().nonEmpty)
  }

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
