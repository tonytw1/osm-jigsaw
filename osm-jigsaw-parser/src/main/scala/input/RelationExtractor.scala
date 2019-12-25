package input

import java.io.InputStream

import model.EntityRendering
import org.apache.logging.log4j.scala.Logging
import org.mapdb.volume.MappedFileVol
import org.mapdb.{Serializer, SortedTableMap}
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import progress.CommaFormattedNumbers
import resolving.{OuterWayResolver, RelationExpander}
import steps.EntitiesToGraph

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable

class RelationExtractor extends Logging with EntityRendering with CommaFormattedNumbers with Extracts
  with EntitiesToGraph {

  private val relationExpander = new RelationExpander()
  private val outerWayResolver = new OuterWayResolver()

  // Given an OSM pbf extract file and a predicate describing the entities we are interested in,
  // extract those entities and their component sub relations, ways and nodes into a new file.
  // Output the component ways and nodes to mapdb volumes
  def extract(extractName: String, predicate: Entity => Boolean, outputFileprefix: String) = {
    val extractRelations = relationsFromExtract(extractName)
    val extractWays = waysFromExtract(extractName)
    val extractNodes = nodesFromExtract(extractName)

    // Build a map of all relations so that it can be used to resolve sub relations
    val allRelations = cacheAllRelations(extractRelations)
    logger.info("Cached " + allRelations.size + " relations")

    // Extract the relations described by the predicate
    logger.info("Extracting relations which match predicate from all relations")
    val foundRelations = allRelations.values.filter(predicate)
    logger.info("Found " + foundRelations.size + " relations to extract")

    // Expand sub relations and record the ways which make up the expanded relations
    logger.info("Resolving relation ways")
    val entityWriter = new OsmWriter(outputFileprefix)
    val relationWayIds = mutable.Set[Long]()
    foundRelations.foreach { r =>
      relationExpander.expandRelation(r, allRelations).map { expanded =>
        entityWriter.write(expanded)
        val outerWayIds = expanded.flatMap { r =>
          outerWayResolver.resolveOuterWayIdsFor(r, allRelations)
        }
        relationWayIds ++= outerWayIds
      }
    }

    logger.info("Need " + commaFormatted(relationWayIds.size) + " ways to resolve relations")
    logger.info("Reading required ways to determine required nodes")

    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && (relationWayIds.contains(entity.getId) || predicate(entity))

    val wayVolume = MappedFileVol.FACTORY.makeVolume(outputFileprefix + ".ways.vol", false)
    val waySink = SortedTableMap.create(
      wayVolume,
      Serializer.LONG,
      Serializer.LONG_ARRAY
    ).createFromSink()

    val nodesRequiredToBuildRequiredWays = mutable.Set[Long]()

    def persistWayAndExpandNodeIds(entity: Entity) = {
      entity match {
        case w: Way =>
          if (predicate(w)) {
            entityWriter.write(w)
          }
          val wayNodeIds = w.getWayNodes.asScala.map(wn => wn.getNodeId)
          waySink.put(w.getId, wayNodeIds.toArray)
          nodesRequiredToBuildRequiredWays ++= wayNodeIds
      }
    }

    new SinkRunner(extractWays, requiredWays, persistWayAndExpandNodeIds).run
    waySink.create()
    wayVolume.close()

    val requiredNodesCount = nodesRequiredToBuildRequiredWays.size
    logger.info("Found ways containing " + commaFormatted(requiredNodesCount) + " nodes")

    logger.info("Need " + commaFormatted(requiredNodesCount) + " nodes to resolve relation ways")
    logger.info("Loading required nodes")

    val nodeVolume = MappedFileVol.FACTORY.makeVolume(outputFileprefix + ".nodes.vol", false)
    val nodeSink = SortedTableMap.create(
      nodeVolume,
      Serializer.LONG,
      Serializer.DOUBLE_ARRAY
    ).createFromSink()

    def allNodes(entity: Entity): Boolean = entity.getType == EntityType.Node

    var foundNodes = 0L

    def addToFoundNodes(entity: Entity) = {
      entity match {
        case n: Node =>
          if (nodesRequiredToBuildRequiredWays.contains(entity.getId)) {
            nodeSink.put(n.getId, Array(n.getLatitude, n.getLongitude))
            foundNodes = foundNodes + 1
          }
          if (hasName(n)) {
            entityWriter.write(n)
          }
      }
    }

    new SinkRunner(extractNodes, allNodes, addToFoundNodes).run
    nodeSink.create()
    nodeVolume.close()

    logger.info("Found " + foundNodes + " nodes")
    entityWriter.close()

    logger.warn("Found " + relationExpander.recursingRelations.size + " infinitely recursing relations: " +
      relationExpander.recursingRelations.mkString(", "))

    logger.info("Extracted: " + commaFormatted(foundRelations.size) + " relations, "
      + commaFormatted(relationWayIds.size) + " ways  and "
      + commaFormatted(requiredNodesCount) + " nodes")
    logger.info(foundRelations.size + " / " + allRelations.size + " of total relations")
    logger.info("Finished outputting selected relations and resolved components to: " + outputFileprefix)
  }

  def recursiveRelations(): Seq[Long] = {
    relationExpander.recursingRelations
  }

  private def cacheAllRelations(relationsInput: InputStream) = {
    var allRelations = LongMap[Relation]()

    def addInAllRelationsMap(entity: Entity) = {
      entity match {
        case r: Relation => allRelations = allRelations + (r.getId -> r)
        case _ =>
      }
    }

    def all(entity: Entity): Boolean = true
    new SinkRunner(relationsInput, all, addInAllRelationsMap).run
    allRelations
  }

}
