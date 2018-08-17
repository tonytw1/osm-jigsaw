package tags

import java.io.BufferedInputStream
import java.lang
import java.net.URL

import javax.inject.{Inject, Singleton}
import model.{OsmId, OsmIdParsing}
import org.mapdb.volume.MappedFileVol
import org.mapdb.{Serializer, SortedTableMap}
import outputtagging.OutputTagging
import play.api.{Configuration, Logger}
import progress.ProgressCounter

@Singleton
class TagService @Inject()(configuration: Configuration) extends OsmIdParsing with EntityNameTags {

  val tagsUrl = new URL(configuration.getString("tags.url").get)
  val tagsVolumeFile = configuration.getString("tags.file").get

  val tagsMap = {
    loadTags(tagsUrl)

    Logger.info("Init'ing tag resolver")

    val volume = MappedFileVol.FACTORY.makeVolume(tagsVolumeFile, true)

    val map: SortedTableMap[lang.Long, Any] = SortedTableMap.open(
      volume,
      Serializer.LONG,
      Serializer.JAVA
    )

    map
  }

  val English = "en"

  def tagsFor(osmId: OsmId): Option[Map[String, String]] = {
    val key = osmId.id.toString + osmId.`type`
    Option(tagsMap.get(key)).map { i =>
      val x: Array[(String, String)] = i.asInstanceOf[Array[(String, String)]]
      x.map { a =>
        val b: (String, String) = a
        b
      }.toMap
    }
  }

  def nameForOsmId(osmId: OsmId, encoding: Option[String] = None): Option[String] = {
    tagsFor(osmId).flatMap { tags =>
      getNameFromTags(tags, encoding.getOrElse(English))
    }
  }

  private def loadTags(tagsFile: URL) = {
    try {

      val tagVolume = MappedFileVol.FACTORY.makeVolume(tagsVolumeFile, false)
      val tagSink = SortedTableMap.create(
        tagVolume,
        Serializer.STRING,
        Serializer.JAVA
      ).createFromSink()

      Logger.info("Reeding tags to tags volume")
      val input = new BufferedInputStream(tagsFile.openStream())
      val counter = new ProgressCounter(step = 10000, label = Some("Reading tags"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input)
          outputTagging.map { ot =>
            val osmId: String = ot.osmId.get
            val keys = ot.keys
            val values = ot.values
            val tuples: Array[(String, String)] = keys.zip(values).toArray
            tagSink.put(osmId, tuples)
          }
          ok = outputTagging.nonEmpty
        }
      }
      input.close()
      Logger.info("Read taggings")
      tagSink.create()
      tagVolume.close()

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
