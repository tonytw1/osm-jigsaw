package tiles

import ch.hsr.geohash.GeoHash
import ch.hsr.geohash.util.TwoGeoHashBoundingBox
import model.Tile
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable
import scala.util.control.Breaks._

class TileGenerator extends Logging {

  def generateTiles(resolution: Int): Seq[Tile] = {
    val planetBoundingBox = new ch.hsr.geohash.BoundingBox(-90, 90, -180, 180) // causes an infinite loop so need breakage below =(
    val tt = TwoGeoHashBoundingBox.withCharacterPrecision(planetBoundingBox, resolution)
    val i = new ch.hsr.geohash.util.BoundingBoxGeoHashIterator(tt)

    val uniqueHashes = mutable.Set[GeoHash]()
    breakable {
      while (i.hasNext) {
        val hash = i.next()
        if (uniqueHashes.contains(hash)) {
          break
        }
        uniqueHashes += hash
      }
    }

    uniqueHashes.map { h =>
      Tile(h.toBase32, h.getBoundingBox)
    }.toSeq
  }

}
