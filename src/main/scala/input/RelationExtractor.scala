package input

import input.sinks.{OsmEntitySink, OsmRelationCachingSink}
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType, Relation, Way}
import resolving.RelationWayResolver

import scala.collection.JavaConverters._

class RelationExtractor {

  private val relationWayResolver = new RelationWayResolver()

  def extract(inputFilePath: String, predicate: Entity => Boolean): (Set[Relation], Set[Entity], Set[Entity]) = {

    def all(entity: Entity): Boolean  = true

    val sink = new OsmRelationCachingSink(all)
    val reader = new OsmReader(inputFilePath, sink)
    reader.read
    val allRelations = sink.relations.toMap
    println("Cached " + allRelations.size + " relations")

    val foundRelations = allRelations.values.filter(predicate).toSet
    println("Found " + foundRelations.size + " admin boundaries")

    val wayIds = foundRelations.flatMap(r =>
      relationWayResolver.resolveOuterWayIdsFor(r, allRelations)
    )

    println("Need " + wayIds.size + " ways to resolve relations")

    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)

    val sink3 = new OsmEntitySink(requiredWays)
    val reader3 = new OsmReader(inputFilePath, sink3)
    reader3.read
    val ways = sink3.found.toSet
    println("Found " + ways.size + " ways")

    val nodeIds = ways.flatMap { e =>
      e match {
        case w: Way => w.getWayNodes.asScala.map(wn => wn.getNodeId).toSet
      }
    }

    println("Need " + nodeIds.size + " nodes to resolve relation ways")

    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)

    val sink4 = new OsmEntitySink(requiredNodes)
    val reader4 = new OsmReader(inputFilePath, sink4)
    reader4.read
    val nodes = sink4.found.toSet
    println("Found " + nodes.size + " nodes")

    (foundRelations, ways, nodes)
  }

}
