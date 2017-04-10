package se.qvantel.generator.utils.property

import java.io.InputStream

import org.scalatest.FunSuite
import se.qvantel.generator.utils.property.config.Config

class CallEDRConfigTest extends FunSuite with Config {

  test("Check that there is a mcc-resources file") {
    try {
      val res = config.getString("gen.countries.file")

      val stream : InputStream = getClass.getResourceAsStream(res)
      val lines = scala.io.Source.fromInputStream( stream ).mkString

      assert(lines.nonEmpty)
      stream.close()
    }
    catch {
      case ex: Exception => {
        fail(ex.getMessage())
      }
    }
  }

}
