package se.qvantel.generator

import java.io.File
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import scala.collection.mutable
import scala.io.Source
import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.product.{Product, Point}
import com.typesafe.scalalogging.LazyLogging
import se.qvantel.generator.utils.property.config.ApplicationConfig
import de.ummels.prioritymap.PriorityMap
import scala.util.Random

object Trends extends ApplicationConfig with LazyLogging {
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

  def readTrendsFromFile(startTs: DateTime) : PriorityMap[Product, Long] = {
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

    val temp = files.map(f => (parseTrendFromFile(f.toString), startTs.getMillis)).toMap

    // Return priority map
    PriorityMap(temp.toList: _*)
  }
  /**
   *   From a list of points and an hour, return the points prior and after the hour specified
   */
  def getNextPrevPoints(points: List[Point], hour: Double): (Point, Point) = {
    points.reverse.dropWhile(p => p.trendHour > hour).headOption match {
      case Some(tailPoint) => (tailPoint, points.find(p => p.trendHour > hour).getOrElse(points.head))
      case None => (points.last, points.head)
    }
  }

  /*
   * Find out how long we should sleep for when the next event by points from unknown product
   */
  private def getSleepTimeFromPoints(prevNextPoints: (Point, Point), tsNs: Long, hour: Double): Long ={
    val ts = new DateTime(tsNs / 1000000, DateTimeZone.UTC)
    val prevPoint = prevNextPoints._1
    val nextPoint = prevNextPoints._2

    var sleepNs : Long = 0
    if (prevPoint.cdrPerSec <= 0.0 && nextPoint.cdrPerSec <= 0.0){
      var nextPointTs = ts.withTimeAtStartOfDay.withMillisOfDay((nextPoint.trendHour*60*60*1000).toInt)
      if (nextPointTs.getMillis <= ts.getMillis) {
        nextPointTs = nextPointTs.plusDays(1)
      }
      sleepNs = (nextPointTs.getMillis - ts.getMillis)*1000000
    }
    else {
      var fraction = getFractionBetweenPoints(prevPoint.trendHour, nextPoint.trendHour, hour)
      // Using the fraction and the prev/next points cdr/sec, calculate the cdr/sec at this specific timestamp
      var cdrPerSec = GenerateData.cdrModifier * ((prevPoint.cdrPerSec*(1-fraction)) + (nextPoint.cdrPerSec*fraction))
      // Make sure that cdrPerSec is not extremely low so it doesn't sleep forever
      if (cdrPerSec <= 0.1){
        cdrPerSec = 0.1
      }
      // From the cdrPerSec this timestamp, calculate the sleeptime for the next event
      sleepNs = (1000000000.0 / cdrPerSec).toLong
      // If fraction is <0 or >1 something is wrong, print debug message
      if (fraction < 0 || fraction > 1) {
        logger.error("Fraction has an invalid value!")
      }
    }
    // If sleep is <0 something is wrong, print debug message
    if (sleepNs <= 0) {
      logger.error("Sleep is less or equal to 0!")
    }
    sleepNs
  }

  /**
   * Find out how long we should sleep for when the next cdr event in a specific product trend should be generated
   */
  def nextTrendEventSleep(product: Product, tsNs: Long) : Long = {
    // Get the hour as a double in the timestamp
    val ts = new DateTime(tsNs / 1000000, DateTimeZone.UTC)
    val hour = ts.millisOfDay().get().toDouble/60/60/1000
    // Find the previous and next trend points
    val prevNextPoints = getNextPrevPoints(product.points, hour)
    // Calculate sleep until next event
    val sleep = getSleepTimeFromPoints(prevNextPoints, tsNs, hour)
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

  def changeTrends(maptemp: PriorityMap[Product, Long]) : PriorityMap[Product, Long] = {
    val e = maptemp.map(p => (p._1.copy(points = p._1.points.map(point => point.copy(
      cdrPerSec = (((Random.nextFloat()*0.2) + (-0.1))*point.cdrPerSec) + point.cdrPerSec))), p._2))

    // Return priority map
    PriorityMap(e.toList:_*)
  }
}
