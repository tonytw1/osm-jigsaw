package steps

import java.io.{BufferedOutputStream, FileOutputStream}
import java.util.concurrent.atomic.AtomicInteger

import graphing.{GraphBuilder, GraphWriter}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import progress.ProgressCounter

class SegmentTask(segment: Segment, planet: Area, outputFilename: String, doneCounter: AtomicInteger, total: Int) extends Runnable with Logging {

  override def run(): Unit = {

    val hash = segment.geohash
    val inSegment = segment.areas

    if (inSegment.nonEmpty) {
      val beforeSort = DateTime.now
      val head = new GraphBuilder().buildGraph(planet, inSegment)
      val afterSort = DateTime.now

      val segmentFilename = new FileOutputStream("segments/" + outputFilename + "." + hash.toBase32)
      writeSegmentGraph(head, segmentFilename)

      if (segment.duplicates.nonEmpty) {
        logger.info("steps.Segment has duplicates which also need to be written: " + segment.duplicates.size)
        segment.duplicates.foreach { d =>
          val segmentFilename = new FileOutputStream("segments/" + outputFilename + "." + d.geohash.toBase32)
          writeSegmentGraph(head, segmentFilename)
        }
      }

      val sortingDuration = new Duration(beforeSort, afterSort)
      if (sortingDuration.getStandardSeconds > 1) {
        logger.info(hash.toBase32 + " with " + inSegment.size + " areas sorting duration: " + sortingDuration + " using thread " + Thread.currentThread().getId)
      }
    }

    val done = doneCounter.incrementAndGet()
    logger.info("Progress: " + done + " / " + total)
  }

  private def writeSegmentGraph(head: GraphNode, segmentFilename: FileOutputStream) = {
    logger.debug("Writing segment graph to disk")
    val output = new BufferedOutputStream(segmentFilename)
    val counter = new ProgressCounter(10000)
    new GraphWriter().export(head, output, None, counter)
    output.flush()
    output.close()
  }
}
