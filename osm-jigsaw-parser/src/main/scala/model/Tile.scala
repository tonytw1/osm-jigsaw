package model

import ch.hsr.geohash.BoundingBox

case class Tile(geohash: String, boundingBox: BoundingBox)