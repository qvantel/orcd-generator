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

    assert(prevNext == Trends.getNextPrevPoints(points, 0))
    assert(prevNext == Trends.getNextPrevPoints(points, 1))
    assert(prevNext == Trends.getNextPrevPoints(points, 5))
    assert(prevNext == Trends.getNextPrevPoints(points, 6))
    assert(prevPrev == Trends.getNextPrevPoints(points, 7))
    assert(prevPrev == Trends.getNextPrevPoints(points, 8))
    assert(prevPrev == Trends.getNextPrevPoints(points, 9))
    assert(prevPrev == Trends.getNextPrevPoints(points, 10))
    assert(prevPrev == Trends.getNextPrevPoints(points, 17))
    assert(prevPrev == Trends.getNextPrevPoints(points, 18))
    assert(prevNext == Trends.getNextPrevPoints(points, 19))
    assert(prevNext == Trends.getNextPrevPoints(points, 23))
    assert(prevNext == Trends.getNextPrevPoints(points, 24))
    assert(prevNext == Trends.getNextPrevPoints(points, 25))
  }

  def assertNextTrendEvent(currentTime: String, product: Product, ts: DateTime, fraction: Double, prevPoint: Point, nextPoint: Point) {
    val nextEvent = Trends.nextTrendEventSleep(product, ts)
    val cdrPerSec = (nextPoint.cdrPerSec * (1 - fraction)) + (prevPoint.cdrPerSec * fraction)
    val sleeptime = ((1000 / GenerateData.cdrModifier) / cdrPerSec).toLong
    assert(nextEvent >= sleeptime - (sleeptime / 100000000))
    assert(nextEvent <= sleeptime + (sleeptime / 100000000))
  }

  test("testNextTrendEvent") {
    val prevPoint = Point(6.0, 10.0)
    val nextPoint = Point(18.0, 20.0)
    val points = List(prevPoint, nextPoint)
    val product = Product("a", "b", "c", points)


    var currentTime = "00:00:00"
    var ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    var fraction = 6.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "01:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    fraction = 7.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "02:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    fraction = 8.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "05:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    fraction = 11.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "06:00:00"
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    fraction = 12.0/12.0
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "07:00:00"
    fraction = 11.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "10:00:00"
    fraction = 8.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "13:00:00"
    fraction = 5.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "17:00:00"
    fraction = 1.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "18:00:00"
    fraction = 0.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "19:00:00"
    fraction = 1.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "23:00:00"
    fraction = 5.0/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)

    currentTime = "23:30:00"
    fraction = 5.5/12.0
    ts = DateTime.parse(s"1970-01-01T$currentTime+00:00")
    assertNextTrendEvent(currentTime, product, ts, fraction, prevPoint, nextPoint)
  }
}
