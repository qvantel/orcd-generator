package se.qvantel.generator

import org.scalatest.FunSuite
import se.qvantel.generator.model.product.{Product, Point}
import org.joda.time.{DateTime, DateTimeZone}

class TrendsTest extends FunSuite {

  test("testGetPrevNextPoint"){
    val prevPoint = Point(6.0, 10.0)
    val nextPoint = Point(18.0, 20.0)
    val points = List(prevPoint, nextPoint)
    val prevPrev = (prevPoint, nextPoint)
    val prevNext = (nextPoint, prevPoint)

    assert(Trends.getNextPrevPoints(points, 0)  == prevNext)
    assert(Trends.getNextPrevPoints(points, 1)  == prevNext)
    assert(Trends.getNextPrevPoints(points, 5)  == prevNext)
    assert(Trends.getNextPrevPoints(points, 6)  == prevPrev)
    assert(Trends.getNextPrevPoints(points, 7)  == prevPrev)
    assert(Trends.getNextPrevPoints(points, 8)  == prevPrev)
    assert(Trends.getNextPrevPoints(points, 9)  == prevPrev)
    assert(Trends.getNextPrevPoints(points, 10) == prevPrev)
    assert(Trends.getNextPrevPoints(points, 17) == prevPrev)
    assert(Trends.getNextPrevPoints(points, 18) == prevNext)
    assert(Trends.getNextPrevPoints(points, 19) == prevNext)
    assert(Trends.getNextPrevPoints(points, 23) == prevNext)
    assert(Trends.getNextPrevPoints(points, 24) == prevNext)
    assert(Trends.getNextPrevPoints(points, 25) == prevNext)
  }

  def assertNextTrendEvent(currentTime: String, product: Product, tsUs: Long, fraction: Double, prevPoint: Point, nextPoint: Point) {
    val nextEvent = Trends.nextTrendEventSleep(product, tsUs)
    val cdrPerSec = (nextPoint.cdrPerSec * (1 - fraction)) + (prevPoint.cdrPerSec * fraction)
    val minCdrPerSec = (1000000 / 0.1).toLong
    val sleeptime = (1000000.0 / (GenerateData.cdrModifier * cdrPerSec)).toLong
    assert(nextEvent >= sleeptime - 10000 || nextEvent == minCdrPerSec)
    assert(nextEvent <= sleeptime + 10000 || nextEvent == minCdrPerSec)
  }

  test("testNextTrendEventZeroCDRPerSec") {
    val p1 = Point(2, 0.0)
    val p2 = Point(3, 1000.0)
    val p3 = Point(9, 1000.0)
    val p4 = Point(10.0, 0.0)
    val p5 = Point(13, 0.0)
    val p6 = Point(14, 1000.0)
    val p7 = Point(21, 1000.0)
    val p8 = Point(22, 0.0)
    val points = List(p1, p2, p3, p4, p5, p6, p7, p8)
    val product = Product("a", "b", "c", points)

    var currentTime = "9:00:00"
    var tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p2, p3)

    currentTime = "11:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    val ps = Trends.getNextPrevPoints(points, 11)
    val pp = ps._1.trendHour
    val pn = ps._2.trendHour
    var nextEvent = Trends.nextTrendEventSleep(product, tsUs)
    var sleeptime = 7200000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)

    currentTime = "12:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    nextEvent = Trends.nextTrendEventSleep(product, tsUs)
    sleeptime = 3600000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)

    currentTime = "13:00:01"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p5, p6)

    currentTime = "14:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p6, p7)

    currentTime = "16:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p6, p7)

    currentTime = "18:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p6, p7)

    currentTime = "20:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p6, p7)

    currentTime = "21:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, tsUs, 0, p6, p7)

    currentTime = "22:00:00"
    tsUs = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    nextEvent = Trends.nextTrendEventSleep(product, tsUs)
    sleeptime = 14400000000L
    assert(nextEvent >= sleeptime - 1000)
    assert(nextEvent <= sleeptime + 1000)
  }

  /*
  test("testNextTrendEvent") {
    val prevPoint = Point(6.0, 1000.0)
    val nextPoint = Point(18.0, 2000.0)
    val points = List(prevPoint, nextPoint)
    val product = Product("a", "b", "c", points)

    var currentTime = "00:00:00"
    var ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    var fraction = 6.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "01:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    fraction = 7.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "02:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    fraction = 8.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "05:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    fraction = 11.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "06:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    fraction = 12.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "07:00:00"
    fraction = 11.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "10:00:00"
    fraction = 8.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "13:00:00"
    fraction = 5.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "17:00:00"
    fraction = 1.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "18:01:00"
    fraction = 0.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "19:00:00"
    fraction = 1.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "23:00:00"
    fraction = 5.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "23:30:00"
    fraction = 5.5/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00").getMillis*1000
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)
  }
  */
}
