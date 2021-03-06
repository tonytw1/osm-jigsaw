package tags

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.{Inject, Singleton}

import model.{OsmId, OsmIdParsing}
import outputtagging.OutputTagging
import play.api.{Configuration, Logger}
import progress.ProgressCounter

import scala.collection.{immutable, mutable}

@Singleton
class TagService @Inject()(configuration: Configuration) extends OsmIdParsing with EntityNameTags {

  val tagsFile = {
    val dataUrl = configuration.getString("data.url").get
    val extractName = configuration.getString("extract.name").get
    new URL(dataUrl + "/" + extractName + "/" + extractName + ".tags.pbf")
  }

  val tagsMap: (Map[OsmId, Seq[(Int, String)]], immutable.IndexedSeq[String]) = loadTags(tagsFile)

  val English = "en"

  def tagsFor(osmId: OsmId): Option[Map[String, String]] = {
    val keysIndex = tagsMap._2    // TODO push up

    tagsMap._1.get(osmId).map { i =>
      i.map( j => (keysIndex(j._1), j._2)).toMap
    }
  }

  def nameForOsmId(osmId: OsmId, encoding: Option[String] = None): Option[String] = {
    tagsFor(osmId).flatMap { tags =>
      getNameFromTags(tags, encoding.getOrElse(English))
    }
  }

  private def loadTags(tagsFile: URL): (Map[OsmId, Seq[(Int, String)]], immutable.IndexedSeq[String]) = {
    try {
      val uniqueKeys = mutable.Set[String]()

      var totalKeys = 0

      val input = new BufferedInputStream(tagsFile.openStream())
      val counter = new ProgressCounter(step = 10000, label = Some("Indexing tags"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input)
          outputTagging.foreach { ot =>
            val osmId = ot.osmId.get
            val keys = ot.keys

            keys.map( k => uniqueKeys.add(k))
            totalKeys = totalKeys + keys.size
          }
          ok = outputTagging.nonEmpty
        }
      }
      input.close()

      Logger.info("Found " + totalKeys + " keys of which " + uniqueKeys.size + " were unique")

      Logger.info("Building key and value index maps")
      val keysSeq = uniqueKeys.toIndexedSeq

      val keysIndex: Map[String, Int] = keysSeq.zipWithIndex.toMap

      Logger.info("Rereading tags after indexing")
      val tagsMap = mutable.Map[OsmId, Seq[(Int, String)]]()

      val input2 = new BufferedInputStream(tagsFile.openStream())
      val counter2 = new ProgressCounter(step = 10000, label = Some("Reading tags"))
      ok = true
      while (ok) {
        counter2.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input2)
          outputTagging.map { ot =>
            val osmId = ot.osmId.get
            val keys = ot.keys.map(k => keysIndex.get(k).get)
            val values = ot.values
            val tuples = keys.zip(values).toArray
            tagsMap.put(toOsmId(osmId), tuples)
        }
          ok = outputTagging.nonEmpty
        }
      }
      input2.close()

      Logger.info("Read " + tagsMap.size + " taggings")
      (tagsMap.toMap, keysSeq)

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
