package input

import java.util

import org.joda.time.DateTime
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.openstreetmap.osmosis.core.task.v0_6.Sink

import scala.collection.mutable

class OsmSink(predicate: Entity => Boolean) extends Sink {

  var i = 0
  var j = 0
  var found = mutable.Set[Entity]()
  var low = DateTime.now()

  override def process(entityContainer: EntityContainer): Unit = {
    val entity = entityContainer.getEntity
    if (predicate(entity)) {
      found.add(entity)
    }

    if (j == 1000000) {
      val now = DateTime.now
      // println(now.getMillis - low.getMillis)
      low = now
      j = 0
    }

    i = i + 1
    j = j +1
  }

  override def initialize(metaData: util.Map[String, AnyRef]) = {}

  override def complete() = {}

  override def close() = {
    println("Processed: " + i + " and found " + found.size)
  }

}
