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
      val uniqueKeys = mutable.Set[String]()
      val uniqueValues = mutable.Set[String]()

      var totalKeys = 0
      var totalValues = 0

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

            keys.map( k => uniqueKeys.add(k))
            values.map( k => uniqueValues.add(k))
            totalKeys = totalKeys + keys.size
            totalValues = totalValues + values.size

            output.put(osmId, keys.zip(values))

          }
          ok = outputTagging.nonEmpty
        }
      }

      Logger.info("Read tags: " + output.size)
      Logger.info("Found " + totalKeys + " keys of which " + uniqueKeys.size + " were unique")
      Logger.info("Found " + totalValues + " values of which " + uniqueValues.size + " were unique")
      output.toMap

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
