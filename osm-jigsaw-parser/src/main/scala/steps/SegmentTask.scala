package steps

import java.io.{BufferedOutputStream, FileOutputStream}
import java.util.concurrent.atomic.AtomicInteger

import graphing.{GraphBuilder, GraphWriter}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import output.OutputFiles
import progress.ProgressCounter

class SegmentTask(segment: Segment, extractName: String, planet: Area, doneCounter: AtomicInteger, total: Int) extends Runnable
  with OutputFiles with Logging {

  override def run(): Unit = {
    val hash = segment.geohash
    val inSegment = segment.areas

    if (inSegment.nonEmpty) {
      val beforeSort = DateTime.now
      val head = new GraphBuilder().buildGraph(planet, inSegment)
      val afterSort = DateTime.now

      writeSegmentGraph(head, new FileOutputStream(segmentGraphFile(extractName, segment)))

      if (segment.duplicates.nonEmpty) {
        logger.info("steps.Segment has duplicates which also need to be written: " + segment.duplicates.size)
        segment.duplicates.foreach { d =>
          writeSegmentGraph(head, new FileOutputStream(segmentGraphFile(extractName, d)))
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
