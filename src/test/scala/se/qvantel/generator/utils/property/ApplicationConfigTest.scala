package se.qvantel.generator.utils.property

import org.scalatest.FunSuite
import se.qvantel.generator.utils.property.Config

class ApplicationConfigTest extends FunSuite with Config {

  test("Check that config file is present") {
    val res = config.getString("gen.threaded")
    val contains = res.equals("yes") || res.equals("no")
    assert(contains)
  }

  test("Check that config file has batch size") {
    val res = config.getString("gen.cassandra.element.batch.size").toString.toInt
    assert(res>0)
  }

}
