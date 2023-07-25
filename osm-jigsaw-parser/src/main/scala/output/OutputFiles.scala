package output

trait OutputFiles {

  def areasFilePath(extractName: String, segment: Option[String] = None): String = {
    outputFolderFor(extractName) + "/" + extractName + ".areas" + segment.map(s => "-" + s).getOrElse("") + ".pbf"
  }

  def graphFile(extractName: String) = {
    extractName + ".graph" + ".pbf"
  }

  def graphV2File(extractName: String, segment: Option[String] = None) = {
    outputFolderFor(extractName) + "/" + extractName + ".graphv2" + segment.map(s => "-" + s).getOrElse("") + ".pbf"
  }

  def tagsFilePath(extractName: String, segment: Option[String] = None): String = {
    outputFolderFor(extractName) + "/" + extractName + ".tags" + segment.map(s => "-" + s).getOrElse("") + ".pbf"
  }

  def outputFolderFor(extractName: String): String = {
    extractName
  }

}
