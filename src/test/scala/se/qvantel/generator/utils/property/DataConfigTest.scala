package se.qvantel.generator.utils.property

import org.scalatest.FunSuite
import se.qvantel.generator.utils.property.Config

class DataConfigTest extends FunSuite with Config {

  test("Check that products exists in the column") {
    val products= config.getString("gen.products").split(";")
    assert(products.nonEmpty)
  }
}
