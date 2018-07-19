package model

trait OsmIdParsing {

  def toOsmId(osmId: String): OsmId = {
    OsmId(osmId.dropRight(1).toLong, osmId.takeRight(1).charAt(0))
  }

}
