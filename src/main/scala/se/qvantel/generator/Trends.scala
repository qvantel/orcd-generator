package se.qvantel.generator

import java.io.InputStream
import java.util.TimeZone

import de.ummels.prioritymap.PriorityMap
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import se.qvantel.generator.CDRGenerator.logger
import se.qvantel.generator.model.product.Product
import se.qvantel.generator.utils.Logger
import se.qvantel.generator.utils.property.config.ApplicationConfig


object Trends extends ApplicationConfig with Logger{
  val trends = readTrendsFromFile()

  private def parseTrendFromFile(filename:String) : Product = {

    val filePath = s"/trends/$filename"
    val source : InputStream = getClass.getResourceAsStream(filePath)
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
    val ts = DateTime.now(DateTimeZone.UTC).minusHours(backInTimeHours).getMillis
    logger.info("Back in time hours is set to: " + backInTimeHours)
    PriorityMap(
      parseTrendFromFile("freefacebook.json") -> ts,
      parseTrendFromFile("afterten.json") -> ts,
      parseTrendFromFile("championsleague.json") -> ts,
      parseTrendFromFile("callplannormal.json") -> ts
    )
    //myCampaigns.map(fileName => parseTrendFromFile(fileName))
  }


  def nextTrendEvent(trend: Product, ts: Long) : Long = {
    val currenthour = new DateTime(ts, DateTimeZone.UTC).hourOfDay().get()

    var low = 0.0
    var high = 0.0
    var trendi = 0
    while (high == 0){
      if (trendi >= trend.points.length){
        low = low - 24
        high = trend.points(0).ts
      }
      else {
        if (trend.points(trendi).ts < currenthour) {
          low = trend.points(trendi).ts
        }
        else {
          high = trend.points(trendi).ts
        }
      }
      trendi += 1
    }
    trendi -= 1

    var trendiPrev = trendi - 1
    if (trendi == 0) {
      trendiPrev = trend.points.length-1
    }

    val fraction = (currenthour-low)/(high-low)
    val cdrPrev = trend.points(trendiPrev).cdrPerSec
    val cdrNext = trend.points(trendi).cdrPerSec
    val cdrPerSec = (cdrPrev*(1-fraction)) + (cdrNext*fraction)

    logger.debug(s"$currenthour = $low -> $high = $fraction")
    logger.debug(s"$cdrPrev -> $cdrNext = $cdrPerSec")

    val sleep = (1000/GenerateData.cdrModifier)/cdrPerSec
    logger.info(sleep.toString)
    var next = ts + sleep
    next.toLong
  }
}
