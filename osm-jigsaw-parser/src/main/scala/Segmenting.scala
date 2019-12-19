import Main.{areaOf, boundingBoxFor, logger, makePolygonD, sr}
import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.OperatorDisjoint
import model.Area
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}

trait Segmenting extends Logging {

  def segmentsFor(areas: Seq[Area], hashes: Seq[GeoHash], maxDepth: Int, depth: Int = 1): Seq[(GeoHash, Seq[Area])] = {
    val prefixed = hashes.map (h => h.toBase32.substring(0, depth)).toSet

    prefixed.toSeq.par.flatMap { p =>
      val hash = GeoHash.fromGeohashString(p)
      val hashBase32 = hash.toBase32

      // Find all of th areas which touch this geohash
      val touchingHash: (GeoHash, Seq[Area]) = areasTouchingGeohash(areas, hash)
      logger.info("Geohash " + hashBase32 + " contains: " + touchingHash._2.size)
      if (depth == maxDepth) {
        logger.info("Returning " + hashBase32 + ": " + touchingHash._2.size)
        Seq(touchingHash)
      } else {
        val hashesUnderThisOne: Seq[GeoHash] = hashes.filter(h => h.toBase32.startsWith(hashBase32))
        //logger.info("Stepping down to hashes under " + hashBase32 + ": " + hashesUnderThisOne.map(_.toBase32))
        segmentsFor(touchingHash._2, hashesUnderThisOne, maxDepth, depth + 1)
      }
    }.seq
  }

  private def areasTouchingGeohash(areas: Seq[Area], hash: GeoHash) = {
    val segment = boundingBoxForGeohash(hash)
    val inSegment = areas.filter { a =>
      !OperatorDisjoint.local().execute(segment.polygon, a.polygon, sr, null)
    }
    (hash, inSegment)
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
