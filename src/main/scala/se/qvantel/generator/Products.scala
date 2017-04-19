package se.qvantel.generator

import java.io.File
import de.ummels.prioritymap.PriorityMap
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import scala.collection.mutable
import scala.io.Source
import org.joda.time.DateTime

import se.qvantel.generator.utils.Logger
import se.qvantel.generator.model.product.Product

object Products extends Logger {
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

    val trendsDirPath = Option(System.getProperty("trends.dir")) match {
      case Some(path) => path
      case None => getClass.getClassLoader.getResource("trends").getPath
    }

    logger.info(s"Loading trends from $trendsDirPath")

    val files = recursiveListFiles(new File(trendsDirPath))

    // Create a priority list out of all products with default timestamp
    var pmap = mutable.HashMap.empty[Product, Long]
    files.foreach(f => pmap.put(parseTrendFromFile(f.toString), startTs.getMillis*1000))
    val pmaplist = pmap.toList

    // Return priority map
    PriorityMap(pmap.toList:_*)
  }
}
