package utils.property

import org.scalatest.FunSuite

class ApplicationConfigTest extends FunSuite with Config {

  test("Check that config file is present") {
    val res = config.getString("gen.threaded")
    val contains = if (res.equals("yes") || res.equals("no")) true else false
    assert(contains)
  }

  test("Check that config file has batch size") {
    val res = config.getString("gen.cassandra.element.batch.size").toString.toInt
    assert(res>0)
  }

}
