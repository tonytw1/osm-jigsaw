import java.io._

import areas.AreaComparison
import graphing.{GraphBuilder, GraphReader}
import input.{Extracts, RelationExtractor, SinkRunner}
import model.{Area, AreaIdSequence, EntityRendering}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import outputarea.OutputArea
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import outputtagging.OutputTagging
import outputway.OutputWay
import progress.{CommaFormattedNumbers, ProgressCounter}
import resolving._

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends EntityRendering with Logging with PolygonBuilding with BoundingBox with AreaComparison
  with ProtocolbufferReading with WayJoining with CommaFormattedNumbers with EntityOsmId with Extracts {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def entitiesToGraph(entity: Entity): Boolean = {
    entity match {
        case r: Relation =>  hasName(entity)
        case w: Way => w.isClosed &&  hasName(entity)
        case _ => false
      }
  }

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "stats" => stats(inputFilepath)
      case "boundaries" => findEntityBoundaries(inputFilepath)
      case "split" => split(inputFilepath)
      case "namednodes" => extractNamedNodes(inputFilepath, cmd.getArgList.get(1))
      case "extract" => extract(inputFilepath, cmd.getArgList.get(1))
      case "areaways" => resolveAreaWays(inputFilepath, cmd.getArgList.get(1))
      case "areastats" => areaStats(inputFilepath)
      case "areas" => resolveAreas(inputFilepath, cmd.getArgList.get(1), cmd.getArgList.get(2))
      case "tags" => tags(inputFilepath, cmd.getArgList.get(1), cmd.getArgList.get(2), cmd.getArgList.get(3))
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

  def findEntityBoundaries(inputFilepath: String) {
    var sink: SinkRunner = null
    var currentType: scala.Option[EntityType] = None
    var currentPosition = 0L

    var boundaries: Map[String, Long] = Map.empty
    def scanForBoundaries(entity: Entity) = {
      val entityType = scala.Option(entity.getType)
      if (entityType != currentType) {
        logger.info("Saw first " + entity.getType + " after reading from " + currentPosition)
        boundaries = boundaries + (entity.getType.toString -> currentPosition)
        currentType = entityType
      }
      currentPosition = sink.currentPosition
    }

    def all(entity: Entity): Boolean = true
    sink = new SinkRunner(entireExtract(inputFilepath), all, scanForBoundaries)
    sink.run

    logger.info("Found boundaries: " + boundaries)
    logger.info("Done")
  }

  def split(inputFilepath: String) {
    logger.info("Splitting extract file into relation, way and node files: " + inputFilepath)

    val nodesWriter = new OsmWriter(nodesExtractFilepath(inputFilepath))
    val waysWriter = new OsmWriter(waysExtractFilepath(inputFilepath))
    val relationsWriter = new OsmWriter(relationExtractFilepath(inputFilepath))

    def writeToSplitFiles(entity: Entity) = {
      entity match {
        case n: Node => nodesWriter.write(n)
        case w: Way => waysWriter.write(w)
        case r: Relation => relationsWriter.write(r)
        case _ =>
      }
    }

    def all(entity: Entity): Boolean = true
    val sink = new SinkRunner(entireExtract(inputFilepath), all, writeToSplitFiles)
    sink.run

    nodesWriter.close()
    waysWriter.close()
    relationsWriter.close()
    logger.info("Done")
  }

  def extractNamedNodes(inputFilepath: String, outputFilepath: String): Unit = {
    logger.info("Extracting named nodes")

    val namedNodesOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath: String))

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

  def extract(inputFilepath: String, outputFilepath: String) {
    logger.info("Extracting entities and their resolved components from " + inputFilepath + " into " + outputFilepath)

    new RelationExtractor().extract(inputFilepath, entitiesToGraph, outputFilepath)
    logger.info("Done")
  }

  def extractRelations(inputFilepath: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    logger.info("Extracting specific relations: " + relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    new RelationExtractor().extract(inputFilepath, selectedRelations, outputFilepath)

    logger.info("Done")
  }

  def tags(relationsInputFilepath: String, areasInputPath: String, nodesInputFile: String, outputFilepath: String): Unit = {
    logger.info("Extracting tags for OSM entities used by areas and named nodes")

    val areaOsmIds = readAreaOsmIdsFromPbfFile(areasInputPath)
    val nodeOsmIds = Seq.empty // TODO readNodesOsmIdsFromPbfFile(nodesInputFile)
    val osmIdsInUse = areaOsmIds ++ nodeOsmIds
    logger.info("Found " + osmIdsInUse.size + " OSM ids to extract tags for (" + areaOsmIds.size + " for areas; " + nodeOsmIds.size + " for nodes)")

    def isUse(entity: Entity): Boolean  = {
      osmIdsInUse.contains(osmIdFor(entity))
    }

    var count = 0
    val output = new BufferedOutputStream(new FileOutputStream(outputFilepath))

    def extractTags(entity: Entity) = {
      val keys = entity.getTags.asScala.map(t => t.getKey).toSeq
      val values = entity.getTags.asScala.map(t => t.getValue).toSeq
      OutputTagging(osmId = Some(osmIdFor(entity)), keys = keys, values = values).writeDelimitedTo(output)
      count = count + 1
    }

    new SinkRunner(new FileInputStream(relationsInputFilepath), isUse, extractTags).run
    logger.info("Finished extracting tags")
    output.flush()
    output.close
    logger.info("Dumped " + count + " tags to file: " + outputFilepath)
  }


  def resolveAreaWays(inputFilepath: String, outputFilepath: String): Unit = {

    def all(entity: Entity): Boolean = true

    var relations = LongMap[Relation]()
    var waysToResolve = Set[Way]()

    val entityLoadProgress = new ProgressCounter(step = 100000)
    def loadIntoMemory(entity: Entity) = {
      entityLoadProgress.withProgress {
        entity match {
          case r: Relation => relations = relations + (r.getId -> r)
          case w: Way => waysToResolve = waysToResolve + w
          case _ =>
        }
      }
    }

    logger.info("Loading entities")
    new SinkRunner(new FileInputStream(inputFilepath), all, loadIntoMemory).run
    logger.info("Finished loading entities")

    logger.info("Resolving relation areas")
    val resolvedAreasOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath))

    var counter = 0

    val waysUsed = mutable.Set[model.Way]()

    def outputAreasToFileAndCacheUsedWays(resolvedAreas: Seq[ResolvedArea]): Unit = {
      resolvedAreas.foreach { ra =>
        counter = counter + 1
        waysUsed ++= ra.outline.map(_.way)
        val signedWays = ra.outline.map(w => w.way.id * (if (w.reverse) -1 else 1))
        OutputResolvedArea(id = Some(ra.id), osmId = Some(ra.osmId), ways = signedWays).writeDelimitedTo(resolvedAreasOutput)
      }
    }

    logger.info("Filtering relations to resolve")
    val relationsToResolve = relations.values.filter(e => entitiesToGraph(e))
    logger.info("Resolving areas for " + relationsToResolve.size + " relations")

    val areaResolver = new AreaResolver()
    val wayResolver = new MapDBWayResolver(inputFilepath + ".ways.vol")

    areaResolver.resolveAreas(relationsToResolve, relations, wayResolver, outputAreasToFileAndCacheUsedWays)

    logger.info("Resolving areas for " + commaFormatted(waysToResolve.size) + " ways")
    areaResolver.resolveAreas(waysToResolve, relations, wayResolver, outputAreasToFileAndCacheUsedWays) // TODO why are two sets of ways in scope?
    wayResolver.close()

    resolvedAreasOutput.flush()
    resolvedAreasOutput.close()

    logger.info("Dumped " + commaFormatted(counter) + " areas to file: " + outputFilepath)
    logger.info("Collected " + commaFormatted(waysUsed.size) + " ways in the process")

    logger.info("Resolving points for used ways")
    val nodeResolver = new MapDBNodeResolver(inputFilepath + ".nodes.vol")

    val waysOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath + ".ways.pbf"))

    val wayCounter = new ProgressCounter(10000, total = Some(waysUsed.size))
    waysUsed.foreach { w =>
      wayCounter.withProgress {
        val points = w.nodes.flatMap { nid =>
          nodeResolver.resolvePointForNode(nid) // TODO handle missing node
        }
        val outputWay = OutputWay(id = Some(w.id), latitudes = points.map(i => i._1), longitudes = points.map(i => i._2))
        outputWay.writeDelimitedTo(waysOutput)
      }
    }
    waysOutput.flush()
    waysOutput.close()
    logger.info("Done")
  }

  def resolveAreas(entitiesFilepath: String, areaInputFile: String, outputFilepath: String): Unit = {

    def exportArea(area: Area, output: OutputStream): Unit = {
      val latitudes = mutable.ListBuffer[Double]()
      val longitudes = mutable.ListBuffer[Double]()
      val pointCount = area.polygon.getPointCount - 1
      (0 to pointCount).map { i =>
        val p = area.polygon.getPoint(i)
        latitudes.+=(p.getX)
        longitudes.+=(p.getY)
      }

      OutputArea(id = Some(area.id), osmIds = area.osmIds, latitudes = latitudes, longitudes = longitudes, area = Some(area.area)).writeDelimitedTo(output)
    }

    def buildAreas: Unit = {
      val waysFile: String = areaInputFile + "F.ways.pbf"
      logger.info("Reading area ways from file: " + waysFile)
      val ways = mutable.Map[Long, OutputWay]() // TODO just the points

      def readWay(inputStream: InputStream): scala.Option[OutputWay] = OutputWay.parseDelimitedFrom(inputStream)

      def cacheWay(outputWay: OutputWay)= ways.put(outputWay.id.get, outputWay)

      processPbfFile(waysFile, readWay, cacheWay)

      val counter = new ProgressCounter(1000)
      val areasOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath))

      def populateAreaNodesAndOutputToFile(ra: OutputResolvedArea): Unit = {
        counter.withProgress {
          val outline = ra.ways.flatMap { signedWayId =>
            val l = Math.abs(signedWayId)
            val joined = ways.get(l).map { way =>
              val points = way.latitudes.zip(way.longitudes)
              if (signedWayId < 0) points.reverse else points
            }
            if(joined.isEmpty) {
              logger.warn("Failed to resolve way id: " + l)
            }
            joined
          }

          val outerPoints: Seq[(Double, Double)] = outline.flatten
          polygonForPoints(outerPoints).map { p =>
            exportArea(Area(AreaIdSequence.nextId, p, boundingBoxFor(p), ListBuffer(ra.osmId.get), areaOf(p)), areasOutput)
          }
        }
      } // TODO isolate for reuse in test fixtures

      logger.info("Resolving areas")
      val planetPolygon = makePolygon((-180, 90), (180, -90))
      val planet = Area(0, planetPolygon, boundingBoxFor(planetPolygon), ListBuffer.empty, areaOf(planetPolygon))  // TODO
      exportArea(planet, areasOutput)

      def readResolvedArea(inputStream: InputStream)= OutputResolvedArea.parseDelimitedFrom(inputStream)

      logger.info("Expanding way areas")
      processPbfFile(areaInputFile, readResolvedArea, populateAreaNodesAndOutputToFile)

      areasOutput.flush()
      areasOutput.close()
      logger.info("Dumped areas to file: " + outputFilepath)
    }

    def deduplicate = {
      logger.info("Deduplicating areas")
      def deduplicateAreas(areas: Seq[Area]): Seq[Area] = {
        logger.info("Sorting areas by size")
        val sortedAreas = areas.sortBy(_.area)

        val deduplicatedAreas = mutable.ListBuffer[Area]()

        val deduplicationCounter = new ProgressCounter(1000, Some(areas.size))
        sortedAreas.foreach { a =>
          deduplicationCounter.withProgress {
            var ok = deduplicatedAreas.nonEmpty
            val i = deduplicatedAreas.iterator
            var found: scala.Option[Area] = None
            while (ok) {
              val x = i.next()
              if (x.area == a.area && areaSame(x, a)) {
                found = Some(x)
              }
              ok = x.area >= a.area && i.hasNext
            }

            found.map { e =>
              e.osmIds ++= a.osmIds
            }.getOrElse {
              deduplicatedAreas.+=:(a)
            }
          }
        }
        deduplicatedAreas
      }

      val areas = readAreasFromPbfFile(outputFilepath)
      val deduplicatedAreas = deduplicateAreas(areas)
      logger.info("Deduplicated " + areas.size + " areas to " + deduplicatedAreas.size)

      logger.info("Writing deduplicated areas to file")
      val finalOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath))
      val outputCounter = new ProgressCounter(100000, Some(deduplicatedAreas.size))
      deduplicatedAreas.foreach { a =>
        outputCounter.withProgress {
          exportArea(a, finalOutput)
        }
      }
      finalOutput.flush()
      finalOutput.close()
      logger.info("Wrote deduplicated areas to file: " + outputFilepath)
    }

    buildAreas
    deduplicate
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    var areas = readAreasFromPbfFile(inputFilename)

    logger.info("Building graph")
    var head1 = areas.head
    var drop = areas.drop(1)
    logger.info("Head area: " + head1)
    logger.info("Dropped: " + drop.size)
    val head = new GraphBuilder().buildGraph(head1, drop)

    logger.info("Writing graph to disk")
    val output = new BufferedOutputStream(new FileOutputStream(outputFilename))
    val counter = new ProgressCounter(100000)

    logger.info("Export dump")
    new GraphReader().export(head, output, None, counter)

    output.flush()
    output.close()
    logger.info("Done")
  }

  private def readAreasFromPbfFile(inputFilename: String): Seq[Area] = {
    logger.info("Reading areas")
    var areas = ListBuffer[Area]()
    var withOsm = 0

    def loadArea(outputArea: OutputArea) = {
      outputAreaToArea(outputArea).fold {
        logger.warn("Could not build areas from: " + outputArea)
      } { a =>
        if (a.osmIds.nonEmpty) {
          withOsm = withOsm + 1
        }
        areas = areas += a
      }
    }

    processPbfFile(inputFilename, readArea, loadArea)

    logger.info("Read " + areas.size + " areas")
    logger.info("Of which " + withOsm + " had OSM ids")
    areas.toList
  }

  private def readAreaOsmIdsFromPbfFile(inputFilename: String): Set[String] = {
    val seenOsmIds = mutable.Set[String]()

    def captureOsmId(outputArea: OutputArea) = seenOsmIds ++= outputArea.osmIds

    processPbfFile(inputFilename, readArea, captureOsmId)

    seenOsmIds.toSet
  }

  private def readNodesOsmIdsFromPbfFile(inputFilename: String): Set[String] = {
    val seenOsmIds = mutable.Set[String]()

    def captureOsmId(node: OutputNode) = {
      node.osmId.map { osmId =>
        seenOsmIds += osmId
      }
    }

    def readNode(inputStream: InputStream): scala.Option[OutputNode] = OutputNode.parseDelimitedFrom(inputStream)

    processPbfFile(inputFilename, readNode, captureOsmId)

    seenOsmIds.toSet
  }

  def readArea(inputStream: InputStream): scala.Option[OutputArea] = {
    OutputArea.parseDelimitedFrom(inputStream)
  }

  private def outputAreaToArea(oa: OutputArea): scala.Option[Area] = {
    val points: Seq[(Double, Double)] = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    polygonForPoints(points).map { p =>
      Area(id = oa.id.get, polygon = p, boundingBox = boundingBoxFor(p), osmIds = ListBuffer() ++ oa.osmIds, oa.area.get) // TODO Naked gets outline
    }
  }

}
