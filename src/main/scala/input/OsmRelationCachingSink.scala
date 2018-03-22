package input

import java.text.DecimalFormat
import java.util

import org.joda.time.DateTime
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.openstreetmap.osmosis.core.task.v0_6.Sink

import scala.collection.mutable

class OsmRelationCachingSink(predicate: Entity => Boolean) extends Sink {

  val step = 10000000
  val nf = new DecimalFormat()

  var i = 0
  var j = 0
  var low = DateTime.now()

  case class LatLong(latitude: Double, longitude: Double)

  val relations: mutable.Map[Long, Relation] = mutable.Map()

  override def process(entityContainer: EntityContainer): Unit = {

    val entity = entityContainer.getEntity
    entity match {
      case r: Relation =>
        relations.put(r.getId, r)
      case _ =>
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
    println("Processed: " + i + " entities and matched " + relations.size)
  }

}
