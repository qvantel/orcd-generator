package se.qvantel.generator

import java.io.File
import de.ummels.prioritymap.PriorityMap
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import scala.io.Source
import org.joda.time.DateTime
import com.typesafe.scalalogging.LazyLogging
import model.product.Product

object Products extends LazyLogging {
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
    // List all config files in resources/trends
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles.filter(_.isFile)
      these ++ f.listFiles.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val trendsDirPath = Option(System.getProperty("trends.dir")).
      getOrElse(getClass.getClassLoader.getResource("trends").getPath)

    logger.info(s"Loading trends from $trendsDirPath")

    val files = recursiveListFiles(new File(trendsDirPath))

    // Create a priority list out of all products with default timestamp
    val pmap = files
      .map(f => (parseTrendFromFile(f.toString), startTs.getMillis*1000000))
      .toMap

    // Return priority map
    PriorityMap(pmap.toList:_*)
  }
}
