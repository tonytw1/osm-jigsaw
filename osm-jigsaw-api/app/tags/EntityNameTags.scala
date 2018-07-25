package tags

trait EntityNameTags {

  private val English = "en"  // TODO Push to Accepts header

  def getNameFromTags(tags: Map[String, String]): Option[String] = {
    val preferredName = "name:" + English
    val otherUsableNames = Seq(preferredName, "name")

    val allAvailableNames = tags.filter(t => otherUsableNames.contains(t._1)).toSet

    val preferredNames = allAvailableNames.filter(t => t._1 == preferredName)
    val otherNames = allAvailableNames -- preferredNames

    val bestName = (preferredNames.toSeq.sortBy(t => t._2.length) ++ otherNames.toSeq.sortBy(t => t._2.length)).headOption
    bestName.map { t =>
      t._2
    }
  }

}
