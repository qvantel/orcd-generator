package utils.property

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class ApplicationConfigTest extends FunSuite {

  test("Check that config file is present") {
    val conf = ConfigFactory.load()
    val res = conf.getString("gen.threaded")
    val contains = if (res.equals("yes") || res.equals("no")) true else false
    assert(contains)
  }

  test("Check that config file has batch size") {
    val conf = ConfigFactory.load()
    val res = conf.getString("gen.cassandra.element.batch.size").toString.toInt
    assert(res>0)
  }

}
