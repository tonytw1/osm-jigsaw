package input.sinks

import java.text.DecimalFormat
import java.util

import org.joda.time.DateTime
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.openstreetmap.osmosis.core.task.v0_6.Sink

class OsmEntitySink(predicate: Entity => Boolean, callback: Entity => Unit) extends Sink {

  val step = 1000000
  val nf = new DecimalFormat()

  var i = 0L
  var j = 0
  var matched = 0L
  var low = DateTime.now()

  override def process(entityContainer: EntityContainer): Unit = {
    val entity = entityContainer.getEntity
    if (predicate(entity)) {
      // println("Found: " + entity.getId + entity.getType + " " + entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue))
      matched = matched + 1
      callback(entity)
    }

    if (j == step) {
      val now = DateTime.now
      val delta = now.getMillis - low.getMillis
      val rate = step / (delta * 0.001)
      println(nf.format(i) + ": " + delta + " @ " + nf.format(rate))
      low = now
      j = 0
    }

    i = i + 1
    j = j +1
  }

  override def initialize(metaData: util.Map[String, AnyRef]) = {}

  override def complete() = {}

  override def close() = {
    println("Processed: " + i + " entities and matched " + matched)
  }

}
