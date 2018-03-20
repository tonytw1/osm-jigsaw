package input

import java.util

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.task.v0_6.Sink

import scala.collection.JavaConverters._

class OsmSink extends Sink {

  var i = 0;

  override def process(entityContainer: EntityContainer): Unit = {
    val entity = entityContainer.getEntity

    val tags = entity.getTags.asScala
    val name = tags.find(t => t.getKey == "name").map(tn => tn.getValue)
    val adminLevel = tags.find(t => t.getKey == "admin_level").map(tn => tn.getValue)

    name.map { _ =>
      if (adminLevel == Some("2")) {

        val boundaryTag = tags.find(t => t.getKey == "boundary" && t.getValue == "administrative")
        boundaryTag.map { b =>
          val typeTag =  tags.find(t => t.getKey == "type" && t.getValue == "boundary")
          typeTag.map { t =>
            println(i + ": " + name + " " + adminLevel)
            println(tags)
          }
        }
      }
    }

    i = i + 1
  }

  override def initialize(metaData: util.Map[String, AnyRef]) = {}

  override def complete() = {}

  override def close() = {}

}
