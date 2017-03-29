package se.qvantel.generator

import java.io.File
import scala.io.Source

import de.ummels.prioritymap.PriorityMap
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import se.qvantel.generator.model.product.Product
import se.qvantel.generator.utils.Logger
import se.qvantel.generator.utils.property.config.ApplicationConfig

import scala.collection.mutable
import scala.io.Source


object Trends extends ApplicationConfig with Logger{
  val trends = readTrendsFromFile()

  private def parseTrendFromFile(filename: String) : Product = {
    // Open file
    val source = Source.fromFile(filename)

    // Finally, read the actuals contents into a string.
    val lines = source.mkString

    // For json4s, specify parse format
    implicit val format = DefaultFormats

    // Parse the contents, extract to a list of plans
    val plan = parse(lines.toString()).extract[Product]

    // Close source file
    source.close()

    // Return the gathered plan
    plan
  }

  private def readTrendsFromFile () : PriorityMap[Product, Long] = {
    val ts = DateTime.now(DateTimeZone.UTC).minusHours(backInTimeHours).getMillis
    logger.info("Back in time hours is set to: " + backInTimeHours)

    // List all config files in resorces/trends
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles.filter(_.isFile)
      these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
    }
    val url = getClass.getClassLoader.getResource("trends")
    val files = recursiveListFiles(new File(url.toURI()))

    // Create a priority list out of all products with default timestamp
    var pmap = mutable.HashMap.empty[Product, Long]
    files.foreach(f => pmap.put(parseTrendFromFile(f.toString), ts))
    val pmaplist = pmap.toList

    // Return priority map
    PriorityMap(pmap.toList:_*)
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
