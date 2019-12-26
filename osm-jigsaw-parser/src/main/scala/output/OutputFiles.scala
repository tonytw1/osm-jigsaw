package output

import steps.Segment

trait OutputFiles {

  def areasFilePath(extractName: String): String = {
    extractName + ".areas.pbf"
  }

  def segmentGraphFile(extractName: String, segment: Segment) = {
    "segments/" + extractName + ".graph." + segment.geohash.toBase32 + ".pbf"
  }

  def tagsFilePath(extractName: String): String = {
    extractName + ".tags.pbf"
  }

}
