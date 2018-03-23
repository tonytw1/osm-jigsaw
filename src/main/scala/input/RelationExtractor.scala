package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import resolving.RelationWayResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationExtractor {

  private val relationWayResolver = new RelationWayResolver()

  def extract(inputFilePath: String, predicate: Entity => Boolean): (Set[Relation], Set[Entity], Set[Entity]) = {

    def all(entity: Entity): Boolean  = true

    var relations = mutable.Map[Long, Relation]()
    def addToAllRelations(entity: Entity) = entity match {
      case r: Relation => relations.put(r.getId, r)
      case _ =>
    }
    new SinkRunner(inputFilePath, all, addToAllRelations).run
    val allRelations = relations.toMap
    println("Cached " + allRelations.size + " relations")

    val foundRelations = allRelations.values.filter(predicate).toSet
    println("Found " + foundRelations.size + " admin boundaries")

    val wayIds = foundRelations.flatMap(r =>
      relationWayResolver.resolveOuterWayIdsFor(r, allRelations)
    )

    println("Need " + wayIds.size + " ways to resolve relations")

    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)

    val foundWays = mutable.Set[Entity]()
    def addToFoundWays(entity: Entity) = foundWays.add(entity)
    new SinkRunner(inputFilePath, requiredWays, addToFoundWays).run
    val ways = foundWays.toSet
    println("Found " + ways.size + " ways")

    val nodeIds = ways.flatMap { e =>
      e match {
        case w: Way => w.getWayNodes.asScala.map(wn => wn.getNodeId).toSet
      }
    }

    println("Need " + nodeIds.size + " nodes to resolve relation ways")
    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)
    val foundNodes = mutable.Set[Entity]()
    def addToFoundNodes(entity: Entity) = foundNodes.add(entity)
    new SinkRunner(inputFilePath, requiredNodes, addToFoundNodes)
    val nodes = foundNodes.toSet
    println("Found " + nodes.size + " nodes")

    (foundRelations, ways, nodes)
  }

}
