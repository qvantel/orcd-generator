package se.qvantel.generator

import java.io.InputStream

import de.ummels.prioritymap.PriorityMap
import org.joda.time.DateTime
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import se.qvantel.generator.model.product.Product

object Trends {

  val trends = readTrendsFromFile()

  private def parseTrendFromFile(filename:String) : Product = {

    val source : InputStream = getClass.getResourceAsStream(filename)
    // Finally, read the actuals contents into a string.
    val lines = scala.io.Source.fromInputStream( source ).mkString

    // For json4s, specify parse format
    implicit val format = DefaultFormats

    // Parse the contents, extract to a list of plans
    val plan = parse(lines.toString()).extract[Product]

    // Close source file
    source.close()

    // Return the gathered plan
    plan
  }

  def readTrendsFromFile () : PriorityMap[Product, Long] = {
    //val myCampaigns = List("/freefacebook.json", "/afterten.json", "/championsleague.json", "/call.json")
    val ts = DateTime.now().getMillis
    PriorityMap(
      parseTrendFromFile("/freefacebook.json") -> ts,
      parseTrendFromFile("/afterten.json") -> ts,
      parseTrendFromFile("/championsleague.json") -> ts,
      parseTrendFromFile("/callplannormal.json") -> ts
    )
    //myCampaigns.map(fileName => parseTrendFromFile(fileName))
  }
}
