package input

import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType, Relation, Way}

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationDeferencer {

  def dereference(entites: Set[Entity], inputFilePath: String): Set[Entity] = {

    val alreadyOk = entites.filter(e => e.getType == EntityType.Node)

    val toResolve = entites.flatMap { e =>
      e match {
        case r: Relation =>
          r.getMembers.asScala.map { rm =>
            OsmId(rm.getMemberType, rm.getMemberId)
          }
        case w: Way =>
          w.getWayNodes.asScala.map { wn =>
            OsmId(EntityType.Node, wn.getNodeId)
          }
        case _ =>
          Seq()
      }
    }

    if (toResolve.isEmpty) {
      println("Nothing left to resolve; returning: " + alreadyOk.size)
      alreadyOk

    } else {
      println("Entities to resolve: " + toResolve.size)
      def theseEntites(entity: Entity): Boolean = toResolve.contains(OsmId(entity.getType, entity.getId))

      val found = mutable.Set[Entity]()
      def callback(entity: Entity) = found.add(entity)

      val sink = new OsmEntitySink(theseEntites, callback)
      val reader = new OsmReader(inputFilePath, sink)
      reader.read
      val resolved = found.toSet

      println("Found " + resolved.size + " resolved entities")

      entites ++ dereference(resolved, inputFilePath)
    }
  }

  case class OsmId(entityType: EntityType, id: Long)

}
