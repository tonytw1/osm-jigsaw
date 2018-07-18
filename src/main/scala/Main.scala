import java.io._

import areas.AreaComparison
import graphing.{GraphBuilder, GraphReader}
import input.{RelationExtractor, SinkRunner}
import model.{Area, EntityRendering}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import outputarea.OutputArea
import outputtagging.OutputTagging
import progress.ProgressCounter
import resolving._

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends EntityRendering with Logging with PolygonBuilding with BoundingBox with AreaComparison {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def entitiesToGraph(entity: Entity): Boolean = {
    (entity.getType == EntityType.Relation || (entity.getType == EntityType.Way && entity.asInstanceOf[Way].isClosed)) &&
      nameFor(entity).nonEmpty && !entity.getTags.asScala.exists(t => t.getKey == "indoor:area")
  }

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "split" => split(inputFilepath)
      case "extract" => extract(inputFilepath, cmd.getArgList.get(1))
      case "areas" => resolveAreas(inputFilepath, cmd.getArgList.get(1))
      case "tags" => tags(inputFilepath, cmd.getArgList.get(1), cmd.getArgList.get(2))
      case "graph" => buildGraph(inputFilepath, cmd.getArgList.get(1))
      case "rels" => {
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      }
      case _ => logger.info("Unknown step") // TODO exit code
    }
  }

  def split(inputFilepath: String) {
    logger.info("Splitting extract file into relation, way and node files: " + inputFilepath)

    val nodesWriter = new OsmWriter(inputFilepath + ".nodes")
    val waysWriter = new OsmWriter(inputFilepath + ".ways")
    val relationsWriter = new OsmWriter(inputFilepath + ".relations")

    def writeToSplitFiles(entity: Entity) = {
      entity match {
        case n: Node => nodesWriter.write(n)
        case w: Way => waysWriter.write(w)
        case r: Relation => relationsWriter.write(r)
        case _ =>
      }
    }

    def all(entity: Entity): Boolean = true
    new SinkRunner(inputFilepath, all, writeToSplitFiles).run

    nodesWriter.close()
    waysWriter.close()
    relationsWriter.close()
    logger.info("Done")
  }

  def extract(inputFilepath: String, outputFilepath: String) {
    logger.info("Extracting entities and their resolved components from " + inputFilepath + " into " + outputFilepath)

    new RelationExtractor().extract(inputFilepath, entitiesToGraph, outputFilepath)
    logger.info("Done")
  }

  def extractRelations(inputFilepath: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    logger.info(relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilepath, selectedRelations, outputFilepath)

    logger.info("Done")
  }

  def tags(relationsInputFilepath: String, areasInputPath: String, outputFilepath: String): Unit = {
    logger.info("Extracting tags for OSM entities used by areas")

    val osmIdsInUse = readAreaOsmIdsFromPbfFile(areasInputPath)
    logger.info("Found " + osmIdsInUse.size + " OSM ids to extract tags for")

    def usedByAnArea(entity: Entity): Boolean  = {
      osmIdsInUse.contains(osmIdFor(entity))
    }

    var count = 0
    val output = new BufferedOutputStream(new FileOutputStream(outputFilepath))

    def extractTags(entity: Entity) = {
      if (entitiesToGraph(entity)) {
        val keys = entity.getTags.asScala.map(t => t.getKey).toSeq
        val values = entity.getTags.asScala.map(t => t.getValue).toSeq
        OutputTagging(osmId = Some(osmIdFor(entity)), keys = keys, values = values).writeDelimitedTo(output)
        count = count + 1
      }
    }
    new SinkRunner(relationsInputFilepath, usedByAnArea, extractTags).run
    logger.info("Finished extracting tags")
    output.flush()
    output.close
    logger.info("Dumped " + count + " tags to file: " + outputFilepath)
  }

  def resolveAreas(inputFilepath: String, outputFilepath: String): Unit = {

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

    def buildAreas = {
      def all(entity: Entity): Boolean = true

      var relations = LongMap[Relation]()
      var waysToResolve = Set[Way]()

      def addToFound(entity: Entity) = {
        entity match {
          case r: Relation => relations = relations + (r.getId -> r)
          case w: Way => waysToResolve = waysToResolve + w
          case _ =>
        }
      }

      logger.info("Loading entities")
      new SinkRunner(inputFilepath, all, addToFound).run
      logger.info("Finished loading entities")

      logger.info("Found " + relations.size + " relations to process")

      logger.info("Resolving areas")
      val oos = new BufferedOutputStream(new FileOutputStream(outputFilepath))

      def callback(newAreas: Seq[Area]): Unit = {
        newAreas.foreach { a =>
          exportArea(a, oos)
        }
      }

      logger.info("Filtering relations to resolve")
      val relationsToResolve = relations.values.filter(e => entitiesToGraph(e))

      val areaResolver = new AreaResolver()
      val wayResolver = new MapDBWayResolver(inputFilepath + ".ways.vol")
      val nodeResolver = new MapDBNodeResolver(inputFilepath + ".nodes.vol")

      val earthPolygon = makePolygon((-180, 90), (180, -90))
      val earth = Area(0, earthPolygon, boundingBoxFor(earthPolygon), ListBuffer.empty, areaOf(earthPolygon))
      exportArea(earth, oos)

      logger.info("Resolving areas for " + relationsToResolve.size + " relations")
      areaResolver.resolveAreas(relationsToResolve, relations, wayResolver, nodeResolver, callback)

      logger.info("Resolving areas for " + waysToResolve.size + " ways")
      areaResolver.resolveAreas(waysToResolve, relations, wayResolver, nodeResolver, callback) // TODO why are two sets of ways in scope?
      wayResolver.close()
      nodeResolver.close()
      oos.flush()
      oos.close
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
              var x = i.next()
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

      logger.info("Writing deduplicated areas to file")
      val finalOutput = new BufferedOutputStream(new FileOutputStream(outputFilepath))
      val outputCounter = new ProgressCounter(100000, Some(deduplicatedAreas.size))
      deduplicatedAreas.foreach { a =>
        outputCounter.withProgress {
          exportArea(a, finalOutput)
        }
      }
      finalOutput.flush()
      finalOutput.close
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

    def loadArea(area: Area) = {
      if (area.osmIds.nonEmpty) {
        withOsm = withOsm + 1
      }
      areas = areas += area
    }

    processAreasFile(inputFilename, loadArea)

    logger.info("Read " + areas.size + " areas")
    logger.info("Of which " + withOsm + " had OSM ids")
    areas.toList
  }

  private def readAreaOsmIdsFromPbfFile(inputFilename: String): Set[String] = {
    val seenOsmIds = mutable.Set[String]()

    def captureOsmId(area: Area) = {
      area.osmIds.map { osmIds =>
        seenOsmIds += osmIds
      }
    }

    processAreasFile(inputFilename, captureOsmId)

    seenOsmIds.toSet
  }

  private def processAreasFile(inputFilename: String, callback: Area => Unit): Unit = {
    val fileInputStream = new BufferedInputStream(new FileInputStream(inputFilename))
    val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
    var ok = true

    while (ok) {
      counter.withProgress {
        val outputArea = OutputArea.parseDelimitedFrom(fileInputStream)
        outputArea.map { oa =>
          val area = outputAreaToArea(oa)
          area.fold {
            logger.warn("Could not build areas from: " + oa)
          } { a =>
            callback(a)
          }
        }
        ok = outputArea.nonEmpty
      }
    }

    fileInputStream.close
  }

  private def outputAreaToArea(oa: OutputArea): scala.Option[Area] = {
    val points: Seq[(Double, Double)] = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    areaForPoints(points).map { p =>
      Area(id = oa.id.get, polygon = p, boundingBox = boundingBoxFor(p), osmIds = ListBuffer() ++ oa.osmIds, oa.area.get) // TODO Naked gets
    }
  }

  def osmIdFor(entity: Entity): String = {  // TODO push to a trait
    entity.getId.toString + entity.getType.toString.take(1).toUpperCase
  }


}
