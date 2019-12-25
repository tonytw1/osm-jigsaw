package model

import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Tag}

import scala.collection.JavaConverters._

trait EntityRendering {

  val acceptableNameTagKeys = Seq("name:en", "name", "addr:housename")

  def render(entity: Entity): String = {
    entity.getId + entity.getType.toString
  }

  def nameFor(entity: Entity): Option[String] = {
    val tags = entity.getTags.asScala

    val availableNameTags = acceptableNameTagKeys.flatMap { k =>
      tags.find(t => t.getKey == k)
    }

    availableNameTags.headOption.map(_.getValue)
  }

}
