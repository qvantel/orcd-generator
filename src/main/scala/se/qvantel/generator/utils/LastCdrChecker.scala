package se.qvantel.generator.utils

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.CDRGenerator._

object LastCdrChecker {
  def getStartTime(): DateTime = {
    val backInTimeTs = DateTime.now(DateTimeZone.UTC).minusHours(backInTimeHours)
    // If sudden crash, look into the last inserted record and begin generating from that timestamp
    val rows = session.execute(
      s"SELECT created_at FROM $keyspace.$cdrTable " +
        "WHERE clustering_key=0 ORDER BY created_at DESC LIMIT 1")
      .all()

    // By default set startTs to backInTimeTs
    // If events exists in cassandra and last event is newer than backInTimeTs, start at lastEventTs
    // This is done in case for example the CDR Generator crashes or is shut down it will continue where it stopped
    val startTs = !rows.isEmpty match {
      case true => {
        val tsUs = rows.get(0).getLong("created_at")
        val lastEventTs = new DateTime(tsUs / 1000, DateTimeZone.UTC)
        logger.info(s"BackInTimeTs: $backInTimeTs")
        logger.info(s"LastEventTs: $lastEventTs")
        lastEventTs.getMillis > backInTimeTs.getMillis match {
          case true => lastEventTs
          case false => backInTimeTs
        }
      }
      case false => backInTimeTs
    }
    startTs
  }
}
