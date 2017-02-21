package utils.property

import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class CallConfigTest extends FunSuite {

  test("Check that there is a mcc-resources file") {
    try {
      val conf = ConfigFactory.load()
      val res = conf.getString("gen.countries.file")

      val source = scala.io.Source.fromFile(res)
      val contents = source.mkString
      source.close()
      assert(contents.length()>0)
    }
    catch {
      case ex: Exception => fail(ex.getMessage)
    }
  }

}
