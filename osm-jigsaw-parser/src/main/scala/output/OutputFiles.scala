package output

import steps.Segment

trait OutputFiles {

  def areasFilePath(extractName: String): String = {
    outputFolderFor(extractName) + "/" + extractName + ".areas.pbf"
  }

  def segmentGraphFile(extractName: String, segment: Segment) = {
    outputFolderFor(extractName) + "/" + extractName + ".graph." + segment.geohash.toBase32 + ".pbf"
  }

  def graphFile(extractName: String) = {
      extractName + ".graph" + ".pbf"
  }

  def graphV2File(extractName: String) = {
    outputFolderFor(extractName) + "/" + extractName + ".graphv2" + ".pbf"
  }

  def tagsFilePath(extractName: String): String = {
    outputFolderFor(extractName) + "/" + extractName + ".tags.pbf"
  }

  def outputFolderFor(extractName: String): String = {
      extractName
  }

}
