package model

import org.openstreetmap.osmosis.core.domain.v0_6.Entity

import scala.collection.JavaConverters._

trait EntityRendering {

  def render(entity: Entity): String = {
    entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue).getOrElse(entity.getId + entity.getType.toString)
  }

}
