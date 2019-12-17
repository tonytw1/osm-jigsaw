import java.io._

import Main.output
import areas.AreaComparison
import ch.hsr.geohash.GeoHash
import ch.hsr.geohash.util.TwoGeoHashBoundingBox
import com.esri.core.geometry.OperatorDisjoint
import graphing.{GraphBuilder, GraphWriter}
import input._
import model.{Area, AreaIdSequence, EntityRendering}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import outputarea.OutputArea
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import outputtagging.OutputTagging
import outputway.OutputWay
import play.api.libs.json.Json
import progress.{CommaFormattedNumbers, ProgressCounter}
import resolving._

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends EntityRendering with Logging with PolygonBuilding with BoundingBox with AreaComparison
  with ProtocolbufferReading with WayJoining with CommaFormattedNumbers with EntityOsmId
  with Extracts with WorkingFiles with Boundaries {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def entitiesToGraph(entity: Entity): Boolean = {
    entity match {
      case r: Relation => hasName(entity)
      case w: Way => w.isClosed && hasName(entity)
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
      case "extract" => extract(inputFilepath)
      case "namednodes" => extractNamedNodes(inputFilepath, cmd.getArgList.get(1))
      case "areaways" => resolveAreaWays(inputFilepath)
      case "areastats" => areaStats(inputFilepath)
      case "areas" => resolveAreas(inputFilepath)
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

  def findEntityBoundaries(extractName: String) {
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

    val stream = entireExtract(extractName)
    sink = new SinkRunner(stream, all, scanForBoundaries)
    sink.run

    val eof = new File(entireExtractFilepath(extractName)).length
    logger.info("EOF: " + eof)
    boundaries = boundaries + ("EOF" -> eof)

    logger.info("Found boundaries: " + boundaries)
    recordBoundaries(extractName, boundaries)
    logger.info("Done")
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

  def resolveAreaWays(extractName: String): Unit = {

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

    val relsInputFilepath = extractedRelsFilepath(extractName)
    logger.info("Loading entities from: " + relsInputFilepath)
    new SinkRunner(new FileInputStream(relsInputFilepath), all, loadIntoMemory).run
    logger.info("Finished loading entities")

    val areawaysFilepath = areaWaysFilepath(extractName)
    logger.info("Resolving relation areas into: " + areawaysFilepath)
    val resolvedAreasOutput = new BufferedOutputStream(new FileOutputStream(areawaysFilepath))

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
    val wayResolver = new MapDBWayResolver(relsInputFilepath + ".ways.vol")

    areaResolver.resolveAreas(relationsToResolve, relations, wayResolver, outputAreasToFileAndCacheUsedWays)

    logger.info("Resolving areas for " + commaFormatted(waysToResolve.size) + " ways")
    areaResolver.resolveAreas(waysToResolve, relations, wayResolver, outputAreasToFileAndCacheUsedWays) // TODO why are two sets of ways in scope?
    wayResolver.close()

    resolvedAreasOutput.flush()
    resolvedAreasOutput.close()

    logger.info("Dumped " + commaFormatted(counter) + " areas to file: " + areawaysFilepath)
    logger.info("Collected " + commaFormatted(waysUsed.size) + " ways in the process")

    logger.info("Resolving points for used ways")
    val nodeResolver = new MapDBNodeResolver(relsInputFilepath + ".nodes.vol")

    val areaWaysOutput = new BufferedOutputStream(new FileOutputStream(areaWaysWaysFilePath(extractName)))

    val wayCounter = new ProgressCounter(10000, total = Some(waysUsed.size))
    waysUsed.foreach { w =>
      wayCounter.withProgress {
        val points = w.nodes.flatMap { nid =>
          nodeResolver.resolvePointForNode(nid) // TODO handle missing node
        }
        val outputWay = OutputWay(id = Some(w.id), latitudes = points.map(i => i._1), longitudes = points.map(i => i._2))
        outputWay.writeDelimitedTo(areaWaysOutput)
      }
    }
    areaWaysOutput.flush()
    areaWaysOutput.close()
    logger.info("Done")
  }

  def resolveAreas(extractName: String): Unit = {
    val areawaysInputFile = areaWaysFilepath(extractName)
    val areasFilepath = areasFilePath(extractName)

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
      val areawaysWaysFilepath = areaWaysWaysFilePath(extractName)

      logger.info("Reading area ways from file: " + areawaysWaysFilepath)
      val ways = mutable.Map[Long, OutputWay]() // TODO just thze points

      def readWay(inputStream: InputStream): scala.Option[OutputWay] = OutputWay.parseDelimitedFrom(inputStream)

      def cacheWay(outputWay: OutputWay) = ways.put(outputWay.id.get, outputWay)

      processPbfFile(areawaysWaysFilepath, readWay, cacheWay)

      val counter = new ProgressCounter(1000)
      val areasOutput = new BufferedOutputStream(new FileOutputStream(areasFilepath))

      def populateAreaNodesAndExportAreasToFile(ra: OutputResolvedArea): Unit = {
        counter.withProgress {
          val outline = ra.ways.flatMap { signedWayId =>
            val l = Math.abs(signedWayId)
            val joined = ways.get(l).map { way =>
              val points = way.latitudes.zip(way.longitudes)
              if (signedWayId < 0) points.reverse else points
            }
            if (joined.isEmpty) {
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
      def readResolvedArea(inputStream: InputStream) = OutputResolvedArea.parseDelimitedFrom(inputStream)

      logger.info("Expanding way areas")
      processPbfFile(areawaysInputFile, readResolvedArea, populateAreaNodesAndExportAreasToFile)

      areasOutput.flush()
      areasOutput.close()
      logger.info("Dumped areas to file: " + areasFilepath)
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

      val areas = readAreasFromPbfFile(areasFilepath)
      val deduplicatedAreas = deduplicateAreas(areas)
      logger.info("Deduplicated " + areas.size + " areas to " + deduplicatedAreas.size)

      logger.info("Writing deduplicated areas to file")
      val finalOutput = new BufferedOutputStream(new FileOutputStream(areasFilepath))
      val outputCounter = new ProgressCounter(100000, Some(deduplicatedAreas.size))
      deduplicatedAreas.foreach { a =>
        outputCounter.withProgress {
          exportArea(a, finalOutput)
        }
      }
      finalOutput.flush()
      finalOutput.close()
      logger.info("Wrote deduplicated areas to file: " + areasFilepath)
    }

    buildAreas
    deduplicate
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    val areas = readAreasFromPbfFile(inputFilename)

    logger.info("Building graph")

    // Partiton

    val headArea = areas.head
    val drop = areas.drop(1)

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
    println("Bounding box: " + bound)

    val bb = new ch.hsr.geohash.BoundingBox(bound._3, bound._1, bound._2, bound._4)

    val tt = TwoGeoHashBoundingBox.withCharacterPrecision(bb, 4)

    val i = new ch.hsr.geohash.util.BoundingBoxGeoHashIterator(tt)
    val hashes = ListBuffer[GeoHash]()
    while (i.hasNext) {
      val hash: GeoHash = i.next()
      hashes :+ hash
    }

    hashes.par.map { hash =>
      val b = hash.getBoundingBox()

      val p = makePolygonD((b.getNorthWestCorner.getLatitude, b.getNorthWestCorner.getLongitude),
        (b.getSouthEastCorner.getLatitude, b.getSouthEastCorner.getLongitude)
      )
      val tuple = boundingBoxFor(p)
      val segment = Area(id = 1L, polygon = p, tuple, area = areaOf(p))

      val inSegment = drop.filter { a =>
        !OperatorDisjoint.local().execute(segment.polygon, a.polygon, sr, null)
      }

      logger.info("Head area: " + segment)
      logger.info("Dropped: " + inSegment.size)
      val head = new GraphBuilder().buildGraph(segment, inSegment)

      logger.info("Writing graph to disk")
      val output = new BufferedOutputStream(new FileOutputStream(outputFilename + hash.toBase32))
      val counter = new ProgressCounter(1000)

      logger.info("Export dump")
      new GraphWriter().export(head, output, None, counter)


      output.flush()
      output.close()
    }


    logger.info("Done")
  }

  // Preform a depth first traversal of the graph
  def output() = {


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
