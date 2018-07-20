package model

import org.openstreetmap.osmosis.core.domain.v0_6.Entity

import scala.collection.JavaConverters._

trait EntityRendering {

  def render(entity: Entity): String = {
    entity.getId + entity.getType.toString
  }

  def nameFor(entity: Entity): Option[String] = {
    val tags = entity.getTags.asScala
    val nameEn = tags.find(t => t.getKey == "name:en")
    val name = tags.find(t => t.getKey == "name")

    val flatten = Seq(nameEn, name).flatten
    flatten.headOption.map(_.getValue)
  }

}
