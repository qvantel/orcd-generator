package se.qvantel.generator

import java.io.InputStream

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.scalatest.FunSuite
import se.qvantel.generator.model.product.Product

class GenerateDataTest extends FunSuite {
  // Test is based of trends/data/ folder in resource directory.
  // Open a source file
  val source : InputStream = getClass.getResourceAsStream("/trends/data/MyReallyGoodCampaign.json")

  // Finally, read the actual contents into a string.
  val lines = scala.io.Source.fromInputStream( source ).mkString

  // For json4s, specify parse format
  implicit val format = DefaultFormats

  // Parse the contents, extract to a list of countries
  val parsedProduct = parse(lines.toString).extract[Product]

  // Close source file
  source.close()


  test("Destination is generated") {
    assert(GenerateData.destination(parsedProduct).contains("000FFFFFF"))
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
