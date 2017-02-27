package utils.property

import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._
import model.Country

import scala.util.{Failure, Success, Try}

trait CallConfig extends ApplicationConfig {
  val countriesFile = config.getString("gen.countries.file")
  val roamingChance = config.getString("gen.roaming.change").toString.toDouble

  /**
    * Tries to open a JSON-file in the classpath with the name as described "countriesFile"
    * The method then tries to parse the contents of that file as JSON using the library Json4s.
    * It will then parse the JSON into the model.Country model, with the properties as described.
    * @return List[Int]
    */
  def getAvailableMccCodes(): List[Int] = {
    val t = Try {
      // Open a source file
      val source = scala.io.Source.fromFile(countriesFile)

      // Try to read mcc-table, using the Country-model
      // Read from the opened file
      val lines = source.mkString

      // For json4s, specify parse format
      implicit val format = DefaultFormats

      // Parse the contents, extract to a list of countries
      val countriesList = parse(lines).extract[List[Country]]

      // Close source file
      source.close()

      // Take the Country.mcc and make it's own list with only the distinct values
      countriesList.map(c => c.mcc.toInt).distinct
    }

    t match {
      case Success(s) => Success(s).value
      case Failure(e) => {
        e.printStackTrace()
        List()
      }
    }
  }

}

