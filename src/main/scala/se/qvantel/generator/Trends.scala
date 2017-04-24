package se.qvantel.generator

import java.io.File
import com.typesafe.scalalogging.LazyLogging
import de.ummels.prioritymap.PriorityMap
import org.joda.time.DateTime
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import se.qvantel.generator.model.product.{Point, Product}
import se.qvantel.generator.utils.property.config.ApplicationConfig
import scala.collection.mutable
import scala.io.Source

object Trends extends ApplicationConfig with LazyLogging {
  private def parseTrendFromFile(filename: String) : Product = {
    // Open file
    val source = Source.fromFile(filename)

    // Finally, read the actuals contents into a string.
    val lines = source.mkString

    // For json4s, specify parse format
    implicit val format = DefaultFormats

    // Parse the contents, extract to a list of plans
    val plan = parse(lines.toString).extract[Product]

    // Close source file
    source.close()

    // Return the gathered plan
    plan
  }

  def readTrendsFromFile (startTs: DateTime) : PriorityMap[Product, Long] = {
    // List all config files in resources/trends
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles.filter(_.isFile)
      these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val trendsDirPath = Option(System.getProperty("trends.dir")) match {
      case Some(path) => path
      case None => getClass.getClassLoader.getResource("trends").getPath
    }

    logger.info(s"Loading trends from $trendsDirPath")

    val files = recursiveListFiles(new File(trendsDirPath))

    // Create a priority list out of all products with default timestamp
    var pmap = mutable.HashMap.empty[Product, Long]
    files.foreach(f => pmap.put(parseTrendFromFile(f.toString), startTs.getMillis))
    val pmaplist = pmap.toList

    // Return priority map
    PriorityMap(pmap.toList:_*)
  }

  /**
   *   From a list of points and an hour, return the points prior and after the hour specified
   */
  def getNextPrevPoints(points: List[Point], hour: Double): (Point, Point) = {
    points.reverse.dropWhile(p => p.trendHour >= hour).headOption match {
      case Some(tailPoint) => (tailPoint, points.find(p => p.trendHour >= hour).getOrElse(points(0)))
      case None => (points(points.length - 1), points(0))
    }
  }

  /**
   * Find out how long we should sleep for when the next cdr event in a specific product trend should be generated
   */
  def nextTrendEventSleep(trend: Product, ts: DateTime) : Long = {
    // Get the hour as a double in the timestamp
    val hour = ts.minuteOfDay().get().toDouble/60
    // Find the previous and next trend points
    val prevNextPoints = getNextPrevPoints(trend.points, hour)
    val prevPoint = prevNextPoints._1
    val nextPoint = prevNextPoints._2

    val fraction = getFractionBetweenPoints(prevPoint.trendHour, nextPoint.trendHour, hour)
    // Using the fraction and the prev/next points cdr/sec, calculate the cdr/sec at this specific timestamp
    val cdrPerSec = (prevPoint.cdrPerSec*(1-fraction)) + (nextPoint.cdrPerSec*fraction)
    // From the cdrPerSec this timestamp, calculate the sleeptime for the next event
    val sleep = (1000/GenerateData.cdrModifier)/cdrPerSec

    var printError = false
    // If fraction is <0 or >1 something is wrong, print debug message
    if (fraction < 0 || fraction > 1) {
      logger.error("Fraction has an invalid value!")
      printError = true
    }
    // If sleep is <0 something is wrong, print debug message
    else if (sleep < 0) {
      logger.error("Sleep is less than 0!")
      printError = true
    }
    if (printError) {
      val hourPrev = prevPoint.trendHour
      val hourNext = nextPoint.trendHour
      logger.error(s"\tHour: $hour = $hourPrev -> $hourNext")
      logger.error(s"\tSleep: $sleep")
    }
    sleep.toLong
  }

  def getFractionBetweenPoints(prevHour: Double, nextHour: Double, currentHour: Double): Double = {
    // Calculate the diff between the previous and next points and calculate a fraction of the current hour between the points
    val (hourDiffLow, hourDiffHigh) = {
      if (prevHour < nextHour) {
        (currentHour - prevHour, nextHour - prevHour)
      } else if (currentHour < prevHour) {
        (24 - prevHour + currentHour, 24 - prevHour + nextHour)
      } else {
        (currentHour - prevHour, 24 - prevHour + nextHour)
      }
    }

    hourDiffLow/hourDiffHigh
  }
}
