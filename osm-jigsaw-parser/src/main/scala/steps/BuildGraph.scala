package steps

import java.util.concurrent.{Executors, ThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import areas.AreaComparison
import ch.hsr.geohash.GeoHash
import ch.hsr.geohash.util.TwoGeoHashBoundingBox
import input.AreaReading
import model.Area
import org.apache.logging.log4j.scala.Logging
import output.OutputFiles

import scala.collection.mutable.ListBuffer

class BuildGraph extends OutputFiles with AreaReading with Segmenting with AreaComparison with Logging {

  def buildGraph(extractName: String) = {
    val areas = readAreasFromPbfFile(areasFilePath(extractName))

    logger.info("Building graph")

    val headArea = areas.head
    val drop = areas

    // Partiton into segments
    val bounds = areas.map { a =>
      boundingBoxFor(a.polygon)
    }

    var bound = areas.head.boundingBox
    bounds.foreach { b =>
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
    logger.info("Bounding box for cover extract: " + bound)

    val bb = new ch.hsr.geohash.BoundingBox(bound._3, bound._1, bound._2, bound._4)

    val segmentSize = 4
    val tt = TwoGeoHashBoundingBox.withCharacterPrecision(bb, segmentSize)

    val i = new ch.hsr.geohash.util.BoundingBoxGeoHashIterator(tt)
    val hashes = ListBuffer[GeoHash]()
    while (i.hasNext) {
      val hash: GeoHash = i.next()
      hashes += hash
    }

    logger.info("Need " + hashes.size + " segments to cover extract bounding box")

    val planetPolygon = makePolygon((-180, 90), (180, -90))
    val planet = Area(0, planetPolygon, boundingBoxFor(planetPolygon), ListBuffer.empty, areaOf(planetPolygon)) // TODO

    val doneCounter = new AtomicInteger(0)

    logger.info("Mapping areas into segments")
    val segments = segmentsFor(drop, hashes, segmentSize)

    logger.info("Deduplicating segments")
    val deduplicatedSegments = deduplicateSegments(segments) // TODO backfill the deduplicated segments

    logger.info("Processing segments")
    val total = deduplicatedSegments.size

    val availableHardwareThreads = Runtime.getRuntime.availableProcessors()
    logger.info("Available processors: " + availableHardwareThreads)
    val executor = Executors.newFixedThreadPool(availableHardwareThreads).asInstanceOf[ThreadPoolExecutor]

    deduplicatedSegments.map { segment =>
      val t = new SegmentTask(segment, extractName, planet, doneCounter, total)
      val value = executor.submit(t)
      value
    }

    logger.info("Requesting shutdown")
    executor.shutdown()

    logger.info("Awaiting shutdown")
    executor.awaitTermination(5, TimeUnit.SECONDS)

    logger.info("Done")
  }

}
