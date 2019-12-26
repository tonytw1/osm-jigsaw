package steps

import java.io.BufferedOutputStream

import input.{AreaReading, Extracts, SinkRunner}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import outputtagging.OutputTagging
import resolving.EntityOsmId

import scala.collection.JavaConverters._

class ExtractAreaTags extends Extracts with AreaReading with EntityOsmId with Logging {

  def tags(extractName: String, outputFilepath: String): Unit = {
    logger.info("Extracting tags for OSM entities used by areas")

    val areaOsmIds = readAreaOsmIdsFromPbfFile(areasFilePath(extractName))
    val osmIdsInUse = areaOsmIds
    logger.info("Found " + osmIdsInUse.size + " OSM ids to extract tags for (" + areaOsmIds.size + " for areas)")

    def isUsed(entity: Entity): Boolean = {
      osmIdsInUse.contains(osmIdFor(entity))
    }

    var count = 0
    val output = new BufferedOutputStream(tagsFile(outputFilepath))

    def extractTags(entity: Entity) = {
      val keys = entity.getTags.asScala.map(t => t.getKey).toSeq
      val values = entity.getTags.asScala.map(t => t.getValue).toSeq
      OutputTagging(osmId = Some(osmIdFor(entity)), keys = keys, values = values).writeDelimitedTo(output)
      count = count + 1
    }

    new SinkRunner(entireExtract(extractName), isUsed, extractTags).run
    logger.info("Finished extracting tags")
    output.flush()
    output.close()
    logger.info("Dumped " + count + " tags to file: " + outputFilepath)
  }

}
