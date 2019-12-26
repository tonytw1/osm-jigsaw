import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadPoolExecutor, TimeUnit}

import areas.AreaComparison
import ch.hsr.geohash.GeoHash
import ch.hsr.geohash.util.TwoGeoHashBoundingBox
import graphing.EntitiesToGraph
import input._
import model.{Area, EntityRendering}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import outputtagging.OutputTagging
import play.api.libs.json.Json
import progress.CommaFormattedNumbers
import resolving._
import steps._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object Main extends EntityRendering with Logging with PolygonBuilding with BoundingBox with AreaComparison
  with ProtocolbufferReading with WayJoining with CommaFormattedNumbers with EntityOsmId
  with Extracts with WorkingFiles with Boundaries with Segmenting with EntitiesToGraph with AreaReading {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "stats" => stats(inputFilepath)
      case "boundaries" => new FindBoundaries().findEntityBoundaries(inputFilepath)
      case "extract" => extract(inputFilepath)
      case "namednodes" => extractNamedNodes(inputFilepath, cmd.getArgList.get(1))
      case "areaways" => new ExtractAreas().resolveAreaWays(inputFilepath)
      case "areastats" => areaStats(inputFilepath)
      case "areas" => new RenderAndDeduplicateAreas().resolveAreas(inputFilepath)
      case "tags" => tags(cmd.getArgList.get(0), cmd.getArgList.get(1))
      case "graph" => buildGraph(inputFilepath, cmd.getArgList.get(1))
      case "rels" => {
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      }
      case _ => logger.info("Unknown step") // TODO exit code
    }
  }

  def stats(inputFilepath: String) = {
    var nodes = 0L
    var ways = 0L
    var relations = 0L

    var namedNodes = 0L
    var namedWays = 0L
    var namedRelations = 0L

    def countTypes(entity: Entity) = {
      entity match {
        case n: Node => nodes = nodes + 1
        case w: Way => ways = ways + 1
        case r: Relation => relations = relations + 1
        case _ =>
      }

      if (hasName(entity)) {
        entity match {
          case n: Node => namedNodes = namedNodes + 1
          case w: Way => namedWays = namedWays + 1
          case r: Relation => namedRelations = namedRelations + 1
          case _ =>
        }
      }
    }

    def all(entity: Entity): Boolean = true

    new SinkRunner(entireExtract(inputFilepath), all, countTypes).run

    logger.info("Nodes: " + namedNodes + " / " + nodes)
    logger.info("Ways: " + namedWays + " / " + ways)
    logger.info("Relations: " + namedRelations + " / " + relations)
  }

  def areaStats(inputFilepath: String) = {

    def print(ra: OutputResolvedArea) = {
      ra.ways.foreach { l =>
        println(ra.id.get + "," + l)
      }
    }

    def read(inputStream: InputStream) = OutputResolvedArea.parseDelimitedFrom(inputStream)

    processPbfFile(inputFilepath, read, print)
  }

  def extractNamedNodes(inputFilepath: String, outputFilepath: String): Unit = {
    logger.info("Extracting named nodes")

    val namedNodesOutput = new BufferedOutputStream(namedNodesFile(outputFilepath))

    def writeToNamedNodesFile(entity: Entity) = {
      entity match {
        case n: Node => OutputNode(osmId = Some(osmIdFor(n)), latitude = Some(n.getLatitude), longitude = Some(n.getLongitude)).writeDelimitedTo(namedNodesOutput)
        case _ =>
      }
    }

    def named(entity: Entity): Boolean = {
      hasName(entity)
    }

    new SinkRunner(nodesFromExtract(inputFilepath), named, writeToNamedNodesFile).run

    namedNodesOutput.flush()
    namedNodesOutput.close()
    logger.info("Done")
  }

  def extract(extractName: String) {

    def recordRecursiveRelations(extractName: String, relationIds: Seq[Long]): Unit = {
      val recursiveRelationsFile = new FileOutputStream(recursiveRelationsFilepath(extractName))
      recursiveRelationsFile.write(Json.toBytes(Json.toJson(relationIds)))
      recursiveRelationsFile.close()
    }

    val outputFilepath = extractedRelsFilepath(extractName)
    logger.info("Extracting entities and their resolved components from " + extractName + " into " + outputFilepath)

    val extractor = new RelationExtractor()
    extractor.extract(extractName, entitiesToGraph, outputFilepath)

    logger.info("Dumping discovered recursive relations")
    recordRecursiveRelations(extractName, extractor.recursiveRelations())

    logger.info("Done")
  }

  def extractRelations(extractName: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    logger.info("Extracting specific relations: " + relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    new RelationExtractor().extract(extractName, selectedRelations, outputFilepath)

    logger.info("Done")
  }

  def tags(extractName: String, outputFilepath: String): Unit = {
    logger.info("Extracting tags for OSM entities used by areas")

    val areaOsmIds = readAreaOsmIdsFromPbfFile(areasFilePath(extractName))
    val osmIdsInUse = areaOsmIds
    logger.info("Found " + osmIdsInUse.size + " OSM ids to extract tags for (" + areaOsmIds.size + " for areas)")

    def isUsed(entity: Entity): Boolean = {
      osmIdsInUse.contains(osmIdFor(entity))
    }

    var count = 0
    val output = new BufferedOutputStream(tagsFile(outputFilepath))

    def extractTags(entity: Entity) = {
      val keys = entity.getTags.asScala.map(t => t.getKey).toSeq
      val values = entity.getTags.asScala.map(t => t.getValue).toSeq
      OutputTagging(osmId = Some(osmIdFor(entity)), keys = keys, values = values).writeDelimitedTo(output)
      count = count + 1
    }

    new SinkRunner(entireExtract(extractName), isUsed, extractTags).run
    logger.info("Finished extracting tags")
    output.flush()
    output.close
    logger.info("Dumped " + count + " tags to file: " + outputFilepath)
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    val areas = readAreasFromPbfFile(inputFilename)

    logger.info("Building graph")

    val headArea = areas.head
    val drop = areas

    // Partiton into segments
    val bounds = areas.map{ a =>
      boundingBoxFor(a.polygon)
    }

    var bound = areas.head.boundingBox
    bounds.foreach{ b =>
      if(b._1 > bound._1) {
        bound = bound.copy(_1 = b._1)
      }
      if(b._2 < bound._2) {
        bound = bound.copy(_2 = b._2)
      }
      if(b._3 < bound._3) {
        bound = bound.copy(_3 = b._3)
      }
      if(b._4 > bound._4) {
        bound = bound.copy(_4 = b._4)
      }
    }
    logger.info("Bounding box for cover extract: " + bound)

    val bb = new ch.hsr.geohash.BoundingBox(bound._3, bound._1, bound._2, bound._4)

    val segmentSize = 4
    val tt = TwoGeoHashBoundingBox.withCharacterPrecision(bb, segmentSize)

    val i = new ch.hsr.geohash.util.BoundingBoxGeoHashIterator(tt)
    val hashes = ListBuffer[GeoHash]()
    while (i.hasNext) {
      val hash: GeoHash = i.next()
      hashes += hash
    }

    logger.info("Need " + hashes.size + " segments to cover extract bounding box")

    val planetPolygon = makePolygon((-180, 90), (180, -90))
    val planet = Area(0, planetPolygon, boundingBoxFor(planetPolygon), ListBuffer.empty, areaOf(planetPolygon)) // TODO

    val doneCounter = new AtomicInteger(0)

    logger.info("Mapping areas into segments")
    val segments = segmentsFor(drop, hashes, segmentSize)

    logger.info("Deduplicating segments")
    val deduplicatedSegments = deduplicateSegments(segments)  // TODO backfill the deduplicated segments

    logger.info("Processing segments")
    val total = deduplicatedSegments.size

    val availableHardwareThreads = Runtime.getRuntime.availableProcessors()
    logger.info("Available processors: " + availableHardwareThreads)
    val executor = Executors.newFixedThreadPool(availableHardwareThreads).asInstanceOf[ThreadPoolExecutor]

    deduplicatedSegments.map { segment =>
      val t = new SegmentTask(segment, planet, outputFilename, doneCounter, total)
      val value = executor.submit(t)
      value
    }

    logger.info("Requesting shutdown")
    executor.shutdown()

    logger.info("Awaiting shutdown")
    executor.awaitTermination(5, TimeUnit.SECONDS)

    logger.info("Done")
  }

}
