package tags

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject

import graph.Area
import outputtagging.OutputTagging
import play.api.{Configuration, Logger}
import progress.ProgressCounter

import scala.collection.mutable

class TagService @Inject()(configuration: Configuration) {

  val tagsFile = new URL(configuration.getString("tags.url").get)

  val tagsMap= loadTags(tagsFile)

  def tagsFor(osmId: String): Option[Seq[(String, String)]] = {
    tagsMap.get(osmId)
  }

  private def loadTags(tagsFile: URL): Map[String, Seq[(String, String)]] = {
    try {
      val output = mutable.Map[String, Seq[(String, String)]]()

      val input = new BufferedInputStream(tagsFile.openStream())

      val areasMap = mutable.Map[Long, Area]()
      val counter = new ProgressCounter(step = 10000, label = Some("Reading tags"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input)
          outputTagging.map { ot =>
            val osmId = ot.osmId.get
            val keys = ot.keys
            val values = ot.values
            output.put(osmId, keys.zip(values))

          }
          ok = outputTagging.nonEmpty
        }
      }

      Logger.info("Read tags: " + output.size)
      output.toMap

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
