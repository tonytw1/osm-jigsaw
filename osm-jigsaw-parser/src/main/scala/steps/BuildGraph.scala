package steps

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadPoolExecutor, TimeUnit}

import areas.AreaComparison
import ch.hsr.geohash.GeoHash
import ch.hsr.geohash.util.TwoGeoHashBoundingBox
import input.AreaReading
import model.Area
import org.apache.logging.log4j.scala.Logging
import output.OutputFiles

import scala.collection.mutable.ListBuffer

class BuildGraph extends OutputFiles with AreaReading with Segmenting with AreaComparison with Logging {

  def buildGraph(extractName: String): Unit = {
    logger.info("Building graph")

    logger.info("Reading areas")
    val areas = readAreasFromPbfFile(areasFilePath(extractName))

    // Partition into segments
    val areaBoundingBoxes = areas.map { a =>
      boundingBoxFor(a.polygon)
    }

    val bound = boundsFor(areaBoundingBoxes)
    logger.info("Bounding box to cover extract: " + bound)

    val segmentSize = 4
    val bb = new ch.hsr.geohash.BoundingBox(bound._3, bound._1, bound._2, bound._4)
    val tt = TwoGeoHashBoundingBox.withCharacterPrecision(bb, segmentSize)
    val i = new ch.hsr.geohash.util.BoundingBoxGeoHashIterator(tt)
    val hashes = ListBuffer[GeoHash]()
    while (i.hasNext) {
      hashes += i.next()
    }
    logger.info("Need " + hashes.size + " segments to cover extract bounding box")

    val planetPolygon = makePolygon((-180, 90), (180, -90))
    val planet = Area(0, planetPolygon, boundingBoxFor(planetPolygon), ListBuffer.empty, areaOf(planetPolygon)) // TODO

    logger.info("Mapping areas into segments")
    val segments = segmentsFor(areas, hashes, segmentSize)

    logger.info("Deduplicating segments")
    val deduplicatedSegments = deduplicateSegments(segments)

    logger.info("Processing segments")
    val total = deduplicatedSegments.size

    val availableHardwareThreads = Runtime.getRuntime.availableProcessors()
    logger.info("Available processors: " + availableHardwareThreads)
    val executor = Executors.newFixedThreadPool(availableHardwareThreads).asInstanceOf[ThreadPoolExecutor]

    val doneCounter = new AtomicInteger(0)
    deduplicatedSegments.map { segment =>
      executor.submit(new SegmentTask(segment, extractName, planet, doneCounter, total))
    }

    logger.info("Requesting shutdown")
    executor.shutdown()

    logger.info("Awaiting shutdown")
    executor.awaitTermination(5, TimeUnit.SECONDS)

    logger.info("Done")
  }

  private def boundsFor(boundingBoxes: Seq[(Double, Double, Double, Double)]): (Double, Double, Double, Double) = {
    var bound: (Double, Double, Double, Double) = boundingBoxes.head
    boundingBoxes.foreach { b =>
      if (b._1 > bound._1) {
        bound = bound.copy(_1 = b._1)
      }
      if (b._2 < bound._2) {
        bound = bound.copy(_2 = b._2)
      }
      if (b._3 < bound._3) {
        bound = bound.copy(_3 = b._3)
      }
      if (b._4 > bound._4) {
        bound = bound.copy(_4 = b._4)
      }
    }
    bound
  }

}
