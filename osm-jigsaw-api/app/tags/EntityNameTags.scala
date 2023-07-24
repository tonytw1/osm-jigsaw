package tags

trait EntityNameTags {

  def getNameFromTags(tags: Map[String, String], encoding: String): Option[String] = {
    val preferredName = "name:" + encoding

    val availablePreferredNames = tags.filter(t => t._1 == preferredName).toSet
    if (availablePreferredNames.nonEmpty) {
      // If multiple options are available for the same encoding use the shortest one
      availablePreferredNames.toSeq.sortBy(t => t._2.length).headOption.map(t => t._2)

    } else {
      val otherUsableNames = Seq("name", "addr:housename")
      val availableOtherNames = otherUsableNames.flatMap { tag =>
        tags.get(tag)
      }
      availableOtherNames.headOption
    }
  }

}
