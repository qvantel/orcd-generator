package utils.property

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class DataConfigTest extends FunSuite {

  test("Check that products exists in the column") {
    val cfg = ConfigFactory.load()
    val products= cfg.getString("gen.products").split(";")
    assert(products.nonEmpty)
  }
}
