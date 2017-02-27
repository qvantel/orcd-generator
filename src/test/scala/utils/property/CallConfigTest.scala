package utils.property

import org.scalatest.FunSuite

class CallConfigTest extends FunSuite with Config {

  test("Check that there is a mcc-resources file") {
    try {
      val res = config.getString("gen.countries.file")

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
