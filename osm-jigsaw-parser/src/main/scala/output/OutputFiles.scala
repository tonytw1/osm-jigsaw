package output

import steps.Segment

trait OutputFiles {

  def areasFilePath(extractName: String, segment: Option[String] = None): String = {
    outputFolderFor(extractName) + "/" + extractName + ".areas" + segment.map(s => "-" + s).getOrElse("") + ".pbf"
  }

  @Deprecated
  def segmentGraphFile(extractName: String, segment: Segment) = {
    outputFolderFor(extractName) + "/" + extractName + ".graph." + segment.geohash.toBase32 + ".pbf"
  }

  def graphFile(extractName: String) = {
    extractName + ".graph" + ".pbf"
  }

  def graphV2File(extractName: String, segment: Option[String] = None) = {
    outputFolderFor(extractName) + "/" + extractName + ".graphv2" + segment.map(s => "-" + s).getOrElse("") + ".pbf"
  }

  def tagsFilePath(extractName: String): String = {
    outputFolderFor(extractName) + "/" + extractName + ".tags.pbf"
  }

  def outputFolderFor(extractName: String): String = {
    extractName
  }

}
