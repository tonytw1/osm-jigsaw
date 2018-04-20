package input

import org.apache.logging.log4j.scala.Logging
import org.mapdb.{Serializer, SortedTableMap}
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import resolving.{MapDBNodeResolver, OuterWayResolver, RelationExpander}

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable
import org.mapdb.volume.MappedFileVol

class RelationExtractor extends Logging {

  private val relationExpander = new RelationExpander()
  private val outerWayResolver = new OuterWayResolver()

  // Given an OSM pbf extract file and a predicate describing the relations we are interested in,
  // scan the input and extract the relations. Resolve the sub relations, ways and nodes required to build
  // these relations. Filter this entities into the output file.
  def extract(inputFilePath: String, predicate: Entity => Boolean, outputFilepath: String) = {
    val writer = new OsmWriter(outputFilepath)

    var allRelations = LongMap[Relation]()
    def addToAllRelations(entity: Entity) = {
        entity match {
          case r: Relation => allRelations = allRelations + (r.getId -> r)
          case _ =>
      }
    }
    def all(entity: Entity): Boolean = true
    new SinkRunner(inputFilePath + ".relations", all, addToAllRelations).run
    logger.info("Cached " + allRelations.size + " relations")

    logger.info("Extracting interesting relations from all relations")
    val foundRelations = allRelations.values.filter(predicate)
    logger.info("Found " + foundRelations.size + " relations to extract")

    logger.info("Resolving relation ways")
    logger.info("Creating relation lookup map")

    val relationWayIds = foundRelations.flatMap { r =>
      val expanded = relationExpander.expandRelation(r, allRelations)
      writer.write(expanded)
      expanded.flatMap { r =>
        outerWayResolver.resolveOuterWayIdsFor(r, allRelations)
      }
    }.toSet

    var extractedWaysCount = relationWayIds.size
    logger.info("Need " + extractedWaysCount + " ways to resolve relations")

    logger.info("Reading required ways to determine required nodes")
    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && (relationWayIds.contains(entity.getId) || predicate(entity))

    val wayVolume = MappedFileVol.FACTORY.makeVolume(outputFilepath + ".ways.vol", false)
    val waySink = SortedTableMap.create(
      wayVolume,
      Serializer.LONG,
      Serializer.LONG_ARRAY
    ).createFromSink()

    val nodeIds = mutable.Set[Long]()
    def persistWayAndExpandNodeIds(entity: Entity) = {
        entity match {
          case w: Way =>
            if (predicate(w)) {
              writer.write(w)
            }
            waySink.put(w.getId, w.getWayNodes.asScala.map(wn => wn.getNodeId).toArray)
            nodeIds.++=(w.getWayNodes.asScala.map(wn => wn.getNodeId))
        }
    }
    new SinkRunner(inputFilePath + ".ways", requiredWays, persistWayAndExpandNodeIds).run
    writer.close()
    waySink.create()
    wayVolume.close()

    var extractedNodesCount = nodeIds.size
    logger.info("Found ways containing " + extractedNodesCount + " nodes")

    logger.info("Need " + extractedNodesCount + " nodes to resolve relation ways")
    logger.info("Loading required nodes")


    val nodeVolume = MappedFileVol.FACTORY.makeVolume(outputFilepath + ".nodes.vol", false)
    val nodeSink = SortedTableMap.create(
      nodeVolume,
      Serializer.LONG,
      Serializer.DOUBLE_ARRAY
    ).createFromSink()

    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)
    var foundNodes = 0L
    def addToFoundNodes(entity: Entity) = {
      entity match {
        case n: Node =>
          nodeSink.put(n.getId, Array(n.getLatitude, n.getLongitude))
          foundNodes = foundNodes + 1
      }
    }
    new SinkRunner(inputFilePath + ".nodes", requiredNodes, addToFoundNodes).run
    nodeSink.create()
    nodeVolume.close()

    logger.info("Found " + foundNodes + " nodes")

    logger.info("relations: " + foundRelations.size + ", ways: " + extractedWaysCount + ", nodes: " + extractedNodesCount)
    logger.info(foundRelations.size + " / " + allRelations.size + " of total relations")
    logger.info("Finished outputing selected relations and resolved components to: " + outputFilepath)
  }

}
