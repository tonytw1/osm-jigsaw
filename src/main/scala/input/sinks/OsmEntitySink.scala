package input.sinks

import java.util

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import progress.ProgressCounter
import org.apache.logging.log4j.scala.Logging


class OsmEntitySink(predicate: Entity => Boolean, callback: Entity => Unit) extends Sink with Logging {

  var matched = 0L
  val counter = new ProgressCounter(10000000)

  override def process(entityContainer: EntityContainer): Unit = {
    counter.withProgress {
      val entity = entityContainer.getEntity
      if (predicate(entity)) {
        // println("Found: " + entity.getId + entity.getType + " " + entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue))
        matched = matched + 1
        callback(entity)
      }
    }
  }

  override def initialize(metaData: util.Map[String, AnyRef]) = {}

  override def complete() = {}

  override def close() = {
    logger.info("Sink matched " + matched + " entities")
  }

}
