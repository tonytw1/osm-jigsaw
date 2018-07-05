package tags

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject

import outputtagging.OutputTagging
import play.api.{Configuration, Logger}
import progress.ProgressCounter

import scala.collection.{immutable, mutable}

class TagService @Inject()(configuration: Configuration) {

  val tagsFile = new URL(configuration.getString("tags.url").get)

  val tagsMap = loadTags(tagsFile)

  def tagsFor(osmId: String): Option[Seq[(String, String)]] = {
    val keysIndex = tagsMap._2    // TODO push up
    val valuesIndex = tagsMap._3    // TODO push up

    tagsMap._1.get(osmId).map { i: Seq[(Int, Int)] =>
      i.map( j => (keysIndex(j._1), valuesIndex(j._2)))
    }
  }

  private def loadTags(tagsFile: URL): (Map[String, Seq[(Int, Int)]], immutable.IndexedSeq[String], immutable.IndexedSeq[String]) = {
    try {
      val uniqueKeys = mutable.Set[String]()
      val uniqueValues = mutable.Set[String]()

      var totalKeys = 0
      var totalValues = 0

      val input = new BufferedInputStream(tagsFile.openStream())
      val counter = new ProgressCounter(step = 10000, label = Some("Indexing tags"))
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
          }
          ok = outputTagging.nonEmpty
        }
      }
      input.close()

      Logger.info("Found " + totalKeys + " keys of which " + uniqueKeys.size + " were unique")
      Logger.info("Found " + totalValues + " values of which " + uniqueValues.size + " were unique")

      Logger.info("Building key and value index maps")
      val keysSeq = uniqueKeys.toIndexedSeq
      val valuesSeq = uniqueValues.toIndexedSeq

      val keysIndex: Map[String, Int] = keysSeq.zipWithIndex.toMap
      val valuesIndex: Map[String, Int] = valuesSeq.zipWithIndex.toMap

      Logger.info("Rereading tags after indexing")
      val tagsMap = mutable.Map[String, Seq[(Int, Int)]]()

      val input2 = new BufferedInputStream(tagsFile.openStream())
      val counter2 = new ProgressCounter(step = 10000, label = Some("Reading area tags"))
      ok = true
      while (ok) {
        counter2.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input2)
          outputTagging.map { ot =>
            val osmId = ot.osmId.get
            val keys: Seq[Int] = ot.keys.map(k => keysIndex.get(k).get)
            val values: Seq[Int] = ot.values.map(v => valuesIndex.get(v).get)
            tagsMap.put(osmId, keys.zip(values))
          }
          ok = outputTagging.nonEmpty
        }
      }
      input2.close()

      (tagsMap.toMap, keysSeq, valuesSeq)

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
