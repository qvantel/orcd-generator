package se.qvantel.generator

import org.scalatest.FunSuite
import se.qvantel.generator.model.product.{CountryConfiguration, Point, Product}
import org.joda.time.{DateTime, DateTimeZone}

class TrendsTest extends FunSuite {
  val milliToNano = 1000000

  test("test getPrevNextPoint"){
    val prevPoint = Point(6.0, 10.0)
    val nextPoint = Point(18.0, 20.0)
    val points = List(prevPoint, nextPoint)
    val prevNext = (prevPoint, nextPoint)
    val nextPrev = (nextPoint, prevPoint)

    assert(Trends.getPrevNextPoints(points,  0) == nextPrev)
    assert(Trends.getPrevNextPoints(points,  1) == nextPrev)
    assert(Trends.getPrevNextPoints(points,  5) == nextPrev)
    assert(Trends.getPrevNextPoints(points,  6) == prevNext)
    assert(Trends.getPrevNextPoints(points,  7) == prevNext)
    assert(Trends.getPrevNextPoints(points,  8) == prevNext)
    assert(Trends.getPrevNextPoints(points,  9) == prevNext)
    assert(Trends.getPrevNextPoints(points, 10) == prevNext)
    assert(Trends.getPrevNextPoints(points, 17) == prevNext)
    assert(Trends.getPrevNextPoints(points, 18) == nextPrev)
    assert(Trends.getPrevNextPoints(points, 19) == nextPrev)
    assert(Trends.getPrevNextPoints(points, 23) == nextPrev)
    assert(Trends.getPrevNextPoints(points, 24) == nextPrev)
    assert(Trends.getPrevNextPoints(points, 25) == nextPrev)
  }

  def assertNextTrendEvent(currentTime: String, product: Product, tsNs: Long, fraction: Double, prevPoint: Point, nextPoint: Point) {
    val nextEventSleep = Trends.nextTrendEventSleep(product, tsNs)
    val cdrPerSec = GenerateData.cdrModifier * ((prevPoint.cdrPerSec*(1-fraction)) + (nextPoint.cdrPerSec*fraction))
    val maxSleep  = (1000000000.0 / 0.1).toLong
    val sleeptime = (1000000000.0 / cdrPerSec).toLong
    if (nextEventSleep < maxSleep) {
      assert(nextEventSleep >= (sleeptime - 10000))
      assert(nextEventSleep <= (sleeptime + 10000))
    }
    else {
      assert(nextEventSleep == maxSleep)
    }
  }
  test("test getFractionBetweenPoints"){
    assert(Trends.getFractionBetweenPoints(1,2,1.5) == 0.5)
    assert(Trends.getFractionBetweenPoints(18,6,1) == 7.0/12)
    assert(Trends.getFractionBetweenPoints(18,6,5) == 11.0/12)
    assert(Trends.getFractionBetweenPoints(18,6,6) == 12.0/12)
  }

  test("test product with point that has zero CDR per sec") {
    val p1 = Point(2, 0.0)
    val p2 = Point(3, 1000.0)
    val p3 = Point(9, 1000.0)
    val p4 = Point(10.0, 0.0)
    val p5 = Point(13, 0.0)
    val p6 = Point(14, 1000.0)
    val p7 = Point(21, 1000.0)
    val p8 = Point(22, 0.0)
    val points = List(p1, p2, p3, p4, p5, p6, p7, p8)
    val product = Product("a", "b", "c", points, 1, List.empty[CountryConfiguration])

    var currentTime = "9:00:00"
    var tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p2, p3)

    currentTime = "11:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    val ps = Trends.getPrevNextPoints(points, 11)
    val pp = ps._1.trendHour
    val pn = ps._2.trendHour
    var nextEvent = Trends.nextTrendEventSleep(product, tsNs)
    var sleeptime = 7200000000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)

    currentTime = "12:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    nextEvent = Trends.nextTrendEventSleep(product, tsNs)
    sleeptime = 3600000000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)

    currentTime = "13:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p5, p6)

    currentTime = "14:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p6, p7)

    currentTime = "16:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p6, p7)

    currentTime = "18:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p6, p7)

    currentTime = "20:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p6, p7)

    currentTime = "21:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, 0, p6, p7)

    currentTime = "22:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    nextEvent = Trends.nextTrendEventSleep(product, tsNs)
    sleeptime = 14400000000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)
  }


  test("test nextTrendEvent") {
    val prevPoint = Point(6.0, 1000.0)
    val nextPoint = Point(18.0, 2000.0)
    val points = List(prevPoint, nextPoint)
    val product = Product("a", "b", "c", points, 1, List.empty[CountryConfiguration])

    var currentTime = "00:00:00"
    var tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    var fraction = 6.0/12.0
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "01:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    fraction = 7.0/12.0
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "02:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    fraction = 8.0/12.0
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "05:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    fraction = 11.0/12.0
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "06:00:00"
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    fraction = 0
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "07:00:00"
    fraction = 1.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "10:00:00"
    fraction = 4.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "13:00:00"
    fraction = 7.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "17:00:00"
    fraction = 11.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "18:01:00"
    fraction = (12-0.0166)/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, prevPoint, nextPoint)

    currentTime = "19:00:00"
    fraction = 1.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "23:00:00"
    fraction = 5.0/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)

    currentTime = "23:30:00"
    fraction = 5.5/12.0
    tsNs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*milliToNano
    assertNextTrendEvent(currentTime, product, tsNs, fraction, nextPoint, prevPoint)
  }
}
