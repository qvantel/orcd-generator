package se.qvantel.generator.utils

import scala.util.Random

object RandomUtils {

  def weightedRandom(items: Seq[(Double, String)]): String = {
    var totalWeight = 0.0d
    items.foreach(i => totalWeight += i._1)

    // Now choose a random item
    var random = Random.nextDouble() * totalWeight
    var selectedIso = ""
    var isoFound = false

    items.foreach(
      i => {
        if(!isoFound) {
          random -= i._1
          if (random <= 0.0d) {
            selectedIso = i._2
            isoFound = true
          }
        }
      }
    )
    selectedIso
  }
}
