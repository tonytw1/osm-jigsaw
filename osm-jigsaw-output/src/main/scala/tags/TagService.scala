package tags

import java.io.BufferedInputStream
import java.net.URL

import model.{OsmId, OsmIdParsing}
import org.apache.logging.log4j.scala.Logging
import outputtagging.OutputTagging
import progress.ProgressCounter

import scala.collection.{immutable, mutable}

class TagService(tagsFile: URL) extends OsmIdParsing with EntityNameTags with Logging {

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

      logger.info("Found " + totalKeys + " keys of which " + uniqueKeys.size + " were unique")

      logger.info("Building key and value index maps")
      val keysSeq = uniqueKeys.toIndexedSeq

      val keysIndex: Map[String, Int] = keysSeq.zipWithIndex.toMap

      logger.info("Rereading tags after indexing")
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

      logger.info("Read " + tagsMap.size + " taggings")
      (tagsMap.toMap, keysSeq)

    } catch {
      case e: Exception =>
        logger.error("Error: " + e)
        throw e
    }
  }

}
