package se.qvantel.generator.utils

import scala.util.Random

object RandomUtils {

  def weightedRandom(items: Seq[(Double, String)]): String = {
    var totalWeight = 0.0d
    items.foreach(i => totalWeight += i._1)

    // Now choose a random item
    var random = Random.nextDouble * totalWeight
    var iterator = 0
    var selectedIso = ""
    var isoFound = false

    items.foreach(
      i => {
        random -= i._1
        if (random <= 0.0d && !isoFound) {
          selectedIso = i._2
          isoFound = true
        }
      }
    )
    selectedIso
  }
}


/*
object RandomUtils {

  def RandomWeighted[T](list: Iterable[(T, Double)])  : T = {
    var sum = 0.0
    list.foreach(tup => sum+=tup._2)

    val randomValue = sum * Random.nextDouble()
    list.map(c => c.copy(_1 = c._1, _2 = ._2 + ._2))
    //(se, 1)
    //(dk, 2)
    //(it, 1)

    //(se, 1)
    //(dk, 3)
    //(it, 4)

    val weightMap

    weightMap.foreach(
      weight => tup._2
      iso => tup._1
      if(weight>=randomValue)
        return iso
    )
  }
}*/