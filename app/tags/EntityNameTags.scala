package tags

trait EntityNameTags {

  private val English = "en"  // TODO Push to Accepts header

  def getNameFromTags(tags: Seq[(String, String)]): Option[String] = {
    val preferredName = "name:" + English
    val otherUsableNames = Seq(preferredName, "name")

    val availableNames = tags.filter(t => otherUsableNames.contains(t._1)).sortBy(t => !(t._1 == preferredName))
    val bestName = availableNames.headOption
    bestName.map { t =>
      t._2
    }
  }

}
