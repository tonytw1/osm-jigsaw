package graphing

import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import scala.collection.JavaConverters._

trait EntitiesToGraph {

  def entitiesToGraph(entity: Entity): Boolean = {
    entity match {
      case r: Relation => hasName(r)
      case w: Way => w.isClosed && hasName(entity)
      case _ => false
    }
  }

  def hasName(entity: Entity): Boolean = {
    entity.getTags.asScala.exists(t => t.getKey == "name" || t.getKey.startsWith("name:") || t.getKey == "addr:housename")
  }

}
