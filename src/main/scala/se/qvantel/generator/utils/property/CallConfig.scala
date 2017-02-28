package se.qvantel.generator.utils.property

import java.io.InputStream
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._
import se.qvantel.generator.model.Country


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
      // Open a source file
      val res = config.getString("gen.countries.file")

      val source : InputStream = getClass.getResourceAsStream(res)
      val lines = scala.io.Source.fromInputStream( source ).mkString

      // Try to read mcc-table, using the Country-model
      // Read from the opened file

      // For json4s, specify parse format
      implicit val format = DefaultFormats

      // Parse the contents, extract to a list of countries
      val countriesList = parse(lines.toString()).extract[List[Country]]

      // Close source file
      source.close()

      // Take the Country.mcc and make it's own list with only the distinct values
      countriesList.map(c => c.mcc.toInt).distinct
    }

  }


