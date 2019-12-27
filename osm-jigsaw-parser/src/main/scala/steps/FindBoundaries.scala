package steps

import java.io.File

import input.{Extracts, SinkRunner}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType}

class FindBoundaries extends Extracts with Logging {

  def findEntityBoundaries(extractName: String) {
    var sink: SinkRunner = null
    var currentType: scala.Option[EntityType] = None
    var currentPosition = 0L

    var boundaries: Map[String, Long] = Map.empty

    def scanForBoundaries(entity: Entity) = {
      val entityType = scala.Option(entity.getType)
      if (entityType != currentType) {
        logger.info("Saw first " + entity.getType + " after reading from " + currentPosition)
        boundaries = boundaries + (entity.getType.toString -> currentPosition)
        currentType = entityType
      }
      currentPosition = sink.currentPosition
    }

    def all(entity: Entity): Boolean = true

    val stream = entireExtract(extractName)
    sink = new SinkRunner(stream, all, scanForBoundaries)
    sink.run

    val eof = new File(entireExtractFilepath(extractName)).length
    logger.info("EOF: " + eof)
    boundaries = boundaries + ("EOF" -> eof)

    logger.info("Found boundaries: " + boundaries)
    recordBoundaries(extractName, boundaries)
    logger.info("Done")
  }

}
