package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import resolving.RelationWayResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationExtractor {

  private val relationWayResolver = new RelationWayResolver()

  def extract(inputFilePath: String, predicate: Entity => Boolean): (Set[Relation], Set[Entity], Set[Entity]) = {

    def all(entity: Entity): Boolean  = true

    val allRelations = mutable.Buffer[Relation]()
    def addToAllRelations(entity: Entity) = entity match {
      case r: Relation => allRelations.+=:(r)
      case _ =>
    }
    new SinkRunner(inputFilePath, all, addToAllRelations).run
    println("Cached " + allRelations.size + " relations")

    val foundRelations = allRelations.filter(predicate).toSet
    println("Found " + foundRelations.size + " admin boundaries")

    val relationsLookUpMap = allRelations.map(r => r.getId -> r).toMap
    val wayIds = foundRelations.flatMap(r =>
      relationWayResolver.resolveOuterWayIdsFor(r, relationsLookUpMap)
    )

    println("Need " + wayIds.size + " ways to resolve relations")

    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)

    val foundWays = mutable.Buffer[Entity]()
    def addToFoundWays(entity: Entity) = foundWays.+=(entity)
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
    val foundNodes = mutable.Buffer[Entity]()
    def addToFoundNodes(entity: Entity) = foundNodes.+=(entity)
    new SinkRunner(inputFilePath, requiredNodes, addToFoundNodes).run
    val nodes = foundNodes.toSet
    println("Found " + nodes.size + " nodes")

    (foundRelations, ways, nodes)
  }

}
