import java.io.{BufferedOutputStream, FileOutputStream}
import java.util.concurrent.atomic.AtomicInteger

import Main.logger
import ch.hsr.geohash.GeoHash
import graphing.{GraphBuilder, GraphWriter}
import model.Area
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import progress.ProgressCounter

class SegmentTask(segment: (GeoHash, Seq[Area]), planet: Area, outputFilename: String, doneCounter: AtomicInteger, total: Int) extends Runnable with Logging {

  override def run(): Unit = {

    val hash = segment._1
    val inSegment = segment._2

    if (inSegment.nonEmpty) {
      val beforeSort = DateTime.now
      val head = new GraphBuilder().buildGraph(planet, inSegment)
      val afterSort = DateTime.now

      logger.debug("Writing graph to disk")
      val output = new BufferedOutputStream(new FileOutputStream("segments/" + outputFilename + "." + hash.toBase32))

      val counter = new ProgressCounter(1000)
      logger.debug("Export dump")
      new GraphWriter().export(head, output, None, counter)

      output.flush()
      output.close()

      val sortingDuration = new Duration(beforeSort, afterSort)
      if (sortingDuration.getStandardSeconds > 1) {
        logger.info(hash.toBase32 + " with " + inSegment.size + " areas sorting duration: " + sortingDuration + " using thread " + Thread.currentThread().getId)
      }


    }
    val done = doneCounter.incrementAndGet()
    logger.info("Progress: " + done + " / " + total)
  }
}
