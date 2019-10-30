package tags

trait EntityNameTags {

  def getNameFromTags(tags: Map[String, String], encoding: String): Option[String] = {
    val preferredNameTag = "name:" + encoding
    val allUsableNameTags = Seq(preferredNameTag, "name", "addr:housename")

    val allAvailableNames = tags.filter(t => allUsableNameTags.contains(t._1)).toSet

    val preferredNames = allAvailableNames.filter(t => t._1 == preferredNameTag)
    val otherNames = allAvailableNames -- preferredNames

    val bestName = (preferredNames.toSeq.sortBy(t => t._2.length) ++ otherNames.toSeq.sortBy(t => t._2.length)).headOption
    bestName.map { t =>
      t._2
    }
  }

}
