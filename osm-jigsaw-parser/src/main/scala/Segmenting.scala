import Main.{areaOf, boundingBoxFor, makePolygonD, sr}
import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.OperatorDisjoint
import model.Area
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class Segment(geohash: GeoHash, areas: Seq[Area])

trait Segmenting extends Logging {

  def segmentsFor(areas: Seq[Area], hashes: Seq[GeoHash], maxDepth: Int, depth: Int = 1): Seq[Segment] = {
    val prefixed = hashes.map (h => h.toBase32.substring(0, depth)).toSet

    prefixed.toSeq.par.flatMap { p =>
      val hash = GeoHash.fromGeohashString(p)
      val hashBase32 = hash.toBase32

      // Find all of the areas which touch this geohash
      val touchingHash = areasTouchingGeohash(areas, hash)
      if (depth == maxDepth) {
        val size = touchingHash.areas.size
        if (size > 1000 ) {
          logger.info("Large segment " + hashBase32 + ": " + size)
        }
        Seq(touchingHash)
      } else {
        val hashesUnderThisOne: Seq[GeoHash] = hashes.filter(h => h.toBase32.startsWith(hashBase32))
        segmentsFor(touchingHash.areas, hashesUnderThisOne, maxDepth, depth + 1)
      }
    }.seq
  }

  def deduplicateSegments(segments: Seq[Segment]): Seq[Segment] = {
    val sortedSegments = segments.sortBy(s => s.areas.map(a => a.id).mkString(","))

    val deduplicatedSegments: mutable.ListBuffer[Segment] = ListBuffer.empty
    var previousSegment: Option[Segment] = None
    sortedSegments.foreach { s =>
      previousSegment.fold {
        logger.info("Adding " + s.geohash.toBase32)
        deduplicatedSegments += s
        logger.info("" + deduplicatedSegments.size)
        previousSegment = Some(s)

      } { ps =>
        val str = s.areas.map(a => a.id).mkString(",")

        val str2 = ps.areas.map(a => a.id).mkString(",")
        if (str == str2) {
          logger.info("Segment " + s.geohash.toBase32 + " is a duplicate of " + ps.geohash.toBase32)
          logger.info(str + " -> " + str2)
        } else {
          logger.info("Adding " + s.geohash.toBase32)
          deduplicatedSegments += s
          logger.info("" + deduplicatedSegments.size)
          previousSegment = Some(s)
        }
      }
    }
    logger.info("Deduplicated segments from " + segments.size + " to " + deduplicatedSegments.size)
    deduplicatedSegments
  }

  private def areasTouchingGeohash(areas: Seq[Area], hash: GeoHash): Segment = {
    val segment = boundingBoxForGeohash(hash)
    val inSegment = areas.filter { a =>
      !OperatorDisjoint.local().execute(segment.polygon, a.polygon, sr, null)
    }
    Segment(geohash = hash, areas = inSegment)
  }

  private def boundingBoxForGeohash(hash: GeoHash): Area = {
    val b = hash.getBoundingBox()

    val p = makePolygonD((b.getNorthWestCorner.getLatitude, b.getNorthWestCorner.getLongitude),
      (b.getSouthEastCorner.getLatitude, b.getSouthEastCorner.getLongitude)
    )
    val tuple = boundingBoxFor(p)
    Area(id = 1L, polygon = p, tuple, area = areaOf(p))
  }
}
