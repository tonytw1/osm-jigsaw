package tags

import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.Point
import com.google.common.cache.CacheBuilder
import model.{OsmId, OsmIdParsing}
import outputtagging.OutputTagging
import play.api.{Configuration, Logger}
import progress.ProgressCounter

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.collection.mutable

@Singleton
class TagService @Inject()(configuration: Configuration) extends OsmIdParsing with EntityNameTags {

  private val dataUrl = configuration.getString("data.url").get
  private val extractName = configuration.getString("extract.name").get

  private val geohashResolution = 3

  private val tagsCache = CacheBuilder.newBuilder()
    .maximumSize(10)
    .build[String, Map[OsmId, Map[String, String]]]

  private val English = "en"

  def tagsFor(osmId: OsmId, point: Point): Option[Map[String, String]] = {
    val geohash = GeoHash.withCharacterPrecision(point.getX, point.getY, geohashResolution)

    val tagsFileURL = if (geohashResolution > 0) {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".tags-" + geohash.toBase32 + ".pbf")
    } else {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".tags.pbf")
    }

    val cached = tagsCache.getIfPresent(tagsFileURL.toExternalForm)
    val maybeTagsForSegment = Option(cached).map { t =>
      Logger.info("Cache hit for " + tagsFileURL.toExternalForm)
      Some(t)
    }.getOrElse {
      val maybeTags = loadTags(tagsFileURL)
      maybeTags.foreach{ t =>
        tagsCache.put(tagsFileURL.toExternalForm, t)
      }
      maybeTags
    }

    maybeTagsForSegment.flatMap(_.get(osmId))
  }

  def nameForOsmId(osmId: OsmId, point: Point, encoding: Option[String] = None): Option[String] = { // TODO put another class
    tagsFor(osmId, point).flatMap { tags =>
      getNameFromTags(tags, encoding.getOrElse(English))
    }
  }

  private def loadTags(tagsFile: URL): Option[Map[OsmId, Map[String, String]]] = {
    try {
      Logger.info("Reading tags")
      val tagsMap = mutable.Map[OsmId, Map[String, String]]()

      val input = new BufferedInputStream(tagsFile.openStream())
      val counter = new ProgressCounter(step = 10000, label = Some("Reading tags"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val outputTagging = OutputTagging.parseDelimitedFrom(input)
          outputTagging.map { ot =>
            val osmId = ot.osmId.get
            val keys = ot.keys
            val values = ot.values
            val tuples = keys.zip(values).toMap
            tagsMap.put(toOsmId(osmId), tuples)
          }
          ok = outputTagging.nonEmpty
        }
      }
      input.close()

      Logger.info("Read " + tagsMap.size + " taggings")
      Some(tagsMap.toMap)

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        None
    }
  }

}
