package tags

import play.api.Logger

trait EntityNameTags {

  private val English = "en"  // TODO Push to Accepts header

  def getNameFromTags(tags: Seq[(String, String)]): Option[String] = {
    val preferredName = "name:" + English
    val otherUsableNames = Seq(preferredName, "name")

    val availableNames = tags.filter(t => otherUsableNames.contains(t._1)).sortBy(t => t._1 == preferredName)
    Logger.info("Available names: " + availableNames)
    val bestName = availableNames.headOption
    bestName.map { t =>
      t._2
    }
  }

}
