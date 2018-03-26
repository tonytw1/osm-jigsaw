package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import resolving.RelationWayResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationExtractor {

  private val relationWayResolver = new RelationWayResolver()

  def extract(inputFilePath: String, predicate: Entity => Boolean, outputFilepath: String) = {

    def all(entity: Entity): Boolean  = true

    val writer = new OsmWriter(outputFilepath)

    val allRelations = mutable.Buffer[Relation]()
    def addToAllRelations(entity: Entity) = entity match {
      case r: Relation => allRelations.+=(r)
      case _ =>
    }
    new SinkRunner(inputFilePath, all, addToAllRelations).run
    println("Cached " + allRelations.size + " relations")

    println("Extracting admin boundaries from all relations")
    val foundRelations = allRelations.filter(predicate).toSet
    println("Found " + foundRelations.size + " admin boundaries")

    println("Writing relations to output file")
    writer.write(foundRelations.toSeq)
    println("Finished writing relations")

    println("Creating relation lookup map")
    val relationsLookUpMap = allRelations.map(r => r.getId -> r).toMap

    println("Resolving relation ways")
    val wayIds = foundRelations.flatMap(r =>
      relationWayResolver.resolveOuterWayIdsFor(r, relationsLookUpMap)
    )

    println("Need " + wayIds.size + " ways to resolve relations")
    println("Reading required ways")
    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)
    val foundWays = mutable.Buffer[Entity]()
    def addToFoundWays(entity: Entity) = foundWays.+=(entity)
    new SinkRunner(inputFilePath, requiredWays, addToFoundWays).run
    val ways = foundWays.toSet
    println("Found " + ways.size + " ways")

    println("Writing ways to output file")
    writer.write(ways.toSeq)
    println("Finished writing ways")

    println("Extract node ids required to resolve ways")
    val nodeIds = ways.flatMap { e =>
      e match {
        case w: Way => w.getWayNodes.asScala.map(wn => wn.getNodeId).toSet
      }
    }
    println("Need " + nodeIds.size + " nodes to resolve relation ways")

    println("Loading required nodes") // TODO could pass this through straight to output file
    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)

    var foundNodes = 0L
    def addToFoundNodes(entity: Entity) = {
      writer.write(entity)
      foundNodes = foundNodes + 1
    }
    new SinkRunner(inputFilePath, requiredNodes, addToFoundNodes).run
    println("Found " + foundNodes + " nodes")

    println("Finished output selected relations and resolved components to: " + outputFilepath)
  }

}
