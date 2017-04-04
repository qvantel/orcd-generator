package se.qvantel.generator

import java.io.File
import scala.io.Source

import de.ummels.prioritymap.PriorityMap
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import se.qvantel.generator.model.product.{Product, Point}
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
      trendsDirPath = getClass.getClassLoader.getResource("trends").getPath
    }
    val files = recursiveListFiles(new File(trendsDirPath))

    // Create a priority list out of all products with default timestamp
    var pmap = mutable.HashMap.empty[Product, Long]
    files.foreach(f => pmap.put(parseTrendFromFile(f.toString), startTs.getMillis))
    val pmaplist = pmap.toList

    // Return priority map
    PriorityMap(pmap.toList:_*)
  }

  def getNextPrevPoints(points: List[Point], hour: Double): Tuple2[Point, Point] ={
    var trendi = -1
    var trendiPrev = -1
    var found = false
    while (!found){
      trendi += 1
      if (trendi == 0){ trendiPrev = points.length-1 }
      else { trendiPrev = trendi-1 }

      if (trendi == points.length){
        trendi = 0
        trendiPrev = points.length-1
        found = true
      }
      else if (points(trendi).ts >= hour) {
        found = true
      }
    }
    Tuple2(points(trendiPrev), points(trendi))
  }

  def nextTrendEvent(trend: Product, ts: Long) : Long = {
    var tsDT = new DateTime(ts, DateTimeZone.UTC)
    val hour = tsDT.minuteOfDay().get().toDouble/60
    val prevNextPoints = getNextPrevPoints(trend.points, hour)
    val prevPoint = prevNextPoints._1
    val nextPoint = prevNextPoints._2

    var hourDiffLow = 0.0
    var hourDiffHigh = 0.0
    if (prevPoint.ts < nextPoint.ts) {
      hourDiffLow  = hour - prevPoint.ts
      hourDiffHigh = nextPoint.ts - prevPoint.ts
    }
    else if (hour < prevPoint.ts) {
      hourDiffLow  = 24 - prevPoint.ts + hour
      hourDiffHigh = 24 - prevPoint.ts + nextPoint.ts
    }
    else {
      hourDiffHigh = 24 - prevPoint.ts + nextPoint.ts
      hourDiffLow = hour - prevPoint.ts
    }
    val fraction = hourDiffLow/hourDiffHigh
    val cdrPerSec = (prevPoint.cdrPerSec*(1-fraction)) + (nextPoint.cdrPerSec*fraction)

    val sleep = (1000/GenerateData.cdrModifier)/cdrPerSec

    if (fraction < 0 || fraction > 1) {
      val hourPrev = prevPoint.ts
      val hourNext = nextPoint.ts
      logger.error("Fraction has an invalid value!")
      logger.error(s"\tHour: $hour = $hourPrev -> $hourNext")
      logger.error(s"\tFraction: $hourDiffLow / $hourDiffHigh = $fraction")
      logger.error(s"\tSleep: $sleep")
    }
    else if (sleep < 0) {
      val hourPrev = prevPoint.ts
      val hourNext = nextPoint.ts
      logger.error("Sleep is less than 0!")
      logger.error(s"\tHour: $hour = $hourPrev -> $hourNext")
      logger.error(s"\tFraction: $hourDiffLow / $hourDiffHigh = $fraction")
      logger.error(s"\tSleep: $sleep")
    }
    (ts + sleep).toLong
  }
}
