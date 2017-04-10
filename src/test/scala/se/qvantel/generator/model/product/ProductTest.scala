package se.qvantel.generator.model.product

import java.io.InputStream
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.scalatest.FunSuite

class ProductTest extends FunSuite {

  test("A product configuration file is loadable from disk and read properly") {
    // Test is based of trends/data/ folder in resource directory.
    // Open a source file
    val source : InputStream = getClass.getResourceAsStream("/trends/data/MyReallyGoodCampaign.json")

    // Finally, read the actuals contents into a string.
    val lines = scala.io.Source.fromInputStream( source ).mkString

    // For json4s, specify parse format
    implicit val format = DefaultFormats

    // Parse the contents, extract to a list of countries
    val parsedProduct = parse(lines.toString()).extract[Product]

    // Close source file
    source.close()

    assert(parsedProduct.campaignId.equals("a3f12ea"))
    assert(parsedProduct.points.length == 4)
    assert(parsedProduct.points.head.trendHour == 4)
    assert(parsedProduct.points.head.cdrPerSec == 20)
    assert(parsedProduct.points(1).trendHour == 12)
    assert(parsedProduct.points(1).cdrPerSec == 340)
  }

}
