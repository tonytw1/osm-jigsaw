package tags

trait EntityNameTags {

  def getNameFromTags(tags: Map[String, String], encoding: String): Option[String] = {
    val preferredName = "name:" + encoding
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
