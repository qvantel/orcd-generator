package se.qvantel.generator.utils.property

import java.io.InputStream

import org.scalatest.FunSuite

class CallConfigTest extends FunSuite with Config {

  test("Check that there is a mcc-resources file") {
    try {
      val res = config.getString("gen.countries.file")

      val stream : InputStream = getClass.getResourceAsStream(res)
      val lines = scala.io.Source.fromInputStream( stream ).mkString

      // Try to read mcc-table, using the Country-model
      // Read from the opened file

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
