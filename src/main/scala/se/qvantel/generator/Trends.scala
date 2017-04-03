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

  def readTrendsFromFile (startTs: DateTime) : PriorityMap[Product, Long] = {
    // List all config files in resorces/trends
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles.filter(_.isFile)
      these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    var trendsDirPath = ""
    if (System.getProperty("trends.dir") != null) {
      trendsDirPath = System.getProperty("trends.dir")
      logger.info(s"Loading trends from $trendsDirPath")
    }
    else {
      trendsDirPath = getClass.getClassLoader.getResource("/trends/").getPath
    }
    val files = recursiveListFiles(new File(trendsDirPath))

    // Create a priority list out of all products with default timestamp
    var pmap = mutable.HashMap.empty[Product, Long]
    files.foreach(f => pmap.put(parseTrendFromFile(f.toString), startTs.getMillis))
    val pmaplist = pmap.toList

    // Return priority map
    PriorityMap(pmap.toList:_*)
  }


  def nextTrendEvent(trend: Product, ts: Long) : Long = {
    var tsDT = new DateTime(ts, DateTimeZone.UTC)
    val currentHour = tsDT.minuteOfDay().get().toDouble/60

    var low = 0.0
    var high = 0.0
    var trendi = -1
    while (high == 0){
      trendi += 1
      if (trendi == trend.points.length-1){
        low = low - 24
        high = trend.points.head.ts
      }
      else {
        if (trend.points(trendi).ts < currentHour) {
          low = trend.points(trendi).ts
        }
        else {
          high = trend.points(trendi).ts
        }
      }
    }

    var trendiPrev = trendi - 1
    if (trendi == 0) {
      trendiPrev = trend.points.length-1
    }

    var hourDiffLow = 0.0
    var hourDiffHigh = 0.0
    if (low >= 0) {
      hourDiffLow  = currentHour - low
      hourDiffHigh = high - low
    }
    else {
      hourDiffLow  = 24 - currentHour - low
      hourDiffHigh = 24 - high - low
    }
    val fraction = hourDiffLow/hourDiffHigh
    val cdrPrev = trend.points(trendiPrev).cdrPerSec
    val cdrNext = trend.points(trendi).cdrPerSec
    val cdrPerSec = (cdrPrev*(1-fraction)) + (cdrNext*fraction)

    val sleep = (1000/GenerateData.cdrModifier)/cdrPerSec
    if (fraction < 0 || fraction > 1) {
      logger.error("Fraction has an invalid value!")
      logger.error(s"\tHour: $currentHour = $low -> $high")
      logger.error(s"\tFraction: $hourDiffLow / $hourDiffHigh = $fraction")
      logger.error(s"\tCDR: $cdrPrev -> $cdrNext = $cdrPerSec")
      logger.error(s"\tSleep: $sleep")
    }
    else if (sleep < 0) {
      logger.error("Sleep is less than 0!")
      logger.error(s"\tHour: $currentHour = $low -> $high")
      logger.error(s"\tFraction: $hourDiffLow / $hourDiffHigh = $fraction")
      logger.error(s"\tCDR: $cdrPrev -> $cdrNext = $cdrPerSec")
      logger.error(s"\tSleep: $sleep")
    }
    var next = ts + sleep
    next.toLong
  }
}
