package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import resolving.RelationWayResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationExtractor {

  private val relationWayResolver = new RelationWayResolver()

  // Given an OSM pbf extract file and a predicate describing the relations we are interested in,
  // scan the input and extract the relations. Resolve the sub relations, ways and nodes required to build
  // these relations. Filter this entities into the output file.
  def extract(inputFilePath: String, predicate: Entity => Boolean, outputFilepath: String) = {

    def all(entity: Entity): Boolean = true

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

    println("Reading required ways to determine required nodes")
    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)

    val nodeIds = mutable.Buffer[Long]()
    def persistWayAndExpandNodeIds(entity: Entity) = {
      entity match {
        case w: Way =>
          writer.write(w)
          val ns = w.getWayNodes.asScala.map(wn => wn.getNodeId)
          nodeIds.++=(ns)
      }
    }
    new SinkRunner(inputFilePath, requiredWays, persistWayAndExpandNodeIds).run
    println("Found ways contains " + nodeIds.size + " nodes")

    println("Need " + nodeIds.size + " nodes to resolve relation ways")
    println("Loading required nodes")
    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)
    var foundNodes = 0L
    def addToFoundNodes(entity: Entity) = {
      writer.write(entity)
      foundNodes = foundNodes + 1
    }
    new SinkRunner(inputFilePath, requiredNodes, addToFoundNodes).run
    println("Found " + foundNodes + " nodes")
    writer.close()

    println("Finished outputing selected relations and resolved components to: " + outputFilepath)
  }

}