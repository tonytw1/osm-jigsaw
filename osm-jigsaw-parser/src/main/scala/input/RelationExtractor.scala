package input

import java.io.FileInputStream

import model.EntityRendering
import org.apache.logging.log4j.scala.Logging
import org.mapdb.volume.MappedFileVol
import org.mapdb.{Serializer, SortedTableMap}
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import progress.CommaFormattedNumbers
import resolving.{OuterWayResolver, RelationExpander}

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable

class RelationExtractor extends Logging with EntityRendering with CommaFormattedNumbers with Extracts {

  private val relationExpander = new RelationExpander()
  private val outerWayResolver = new OuterWayResolver()

  // Given an OSM pbf extract file and a predicate describing the relations we are interested in,
  // extract those relations from the input. Resolve the sub relations, ways and nodes required to build
  // those relations.
  // Output the relations and sub relations to a file.
  // Output the way and node information to mapdb volumes
  def extract(inputFilePath: String, predicate: Entity => Boolean, outputFileprefix: String) = {
    var allRelations = LongMap[Relation]()
    def addToAllRelations(entity: Entity) = {
        entity match {
          case r: Relation => allRelations = allRelations + (r.getId -> r)
          case _ =>
      }
    }
    def all(entity: Entity): Boolean = true
    new SinkRunner(new FileInputStream(relationExtractFilepath(inputFilePath)), all, addToAllRelations).run
    logger.info("Cached " + allRelations.size + " relations")

    logger.info("Extracting interesting relations from all relations")
    val foundRelations = allRelations.values.filter(predicate)
    logger.info("Found " + foundRelations.size + " relations to extract")

    logger.info("Resolving relation ways")
    logger.info("Creating relation lookup map")

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
    new SinkRunner(waysFromExtract(inputFilePath), requiredWays, persistWayAndExpandNodeIds).run
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
    new SinkRunner(nodesFromExtract(inputFilePath), allNodes, addToFoundNodes).run
    nodeSink.create()
    nodeVolume.close()

    logger.info("Found " + foundNodes + " nodes")
    entityWriter.close()

    logger.info("Extracted: " + commaFormatted(foundRelations.size) + ", ways: " + commaFormatted(relationWayIds.size) + ", nodes: " + commaFormatted(requiredNodesCount))
    logger.info(foundRelations.size + " / " + allRelations.size + " of total relations")
    logger.info("Finished outputting selected relations and resolved components to: " + outputFileprefix)
  }

}
