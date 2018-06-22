import java.io._

import graphing.{GraphBuilder, GraphReader}
import input.{RelationExtractor, SinkRunner}
import model.{Area, EntityRendering, GraphNode}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
import outputarea.OutputArea
import progress.ProgressCounter
import resolving._

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap
import scala.collection.mutable

object Main extends EntityRendering with Logging with PolygonBuilding with BoundingBox {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def entitiesToGraph(entity: Entity): Boolean = {

    (entity.getType == EntityType.Relation || (entity.getType == EntityType.Way && entity.asInstanceOf[Way].isClosed)) &&
      nameFor(entity).nonEmpty && !entity.getTags.asScala.exists(t => t.getKey == "indoor:area")

    /*
    val tags = entity.getTags.asScala
    val isAdminLevel = tags.exists(t => t.getKey == "admin_level")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")

    val isLeisurePark = tags.exists(t => tags.exists(t => t.getKey == "leisure"  && t.getValue == "park"))
    val isIsland = tags.exists(t => tags.exists(t => t.getKey == "place"  && t.getValue == "island"))
    val isNationalPark = tags.exists(t => tags.exists(t => t.getKey == "boundary"  && t.getValue == "national_park"))

    (entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag) ||
      ((entity.getType == EntityType.Relation || entity.getType == EntityType.Way && entity.asInstanceOf[Way].isClosed) && isLeisurePark || isNationalPark || isIsland)
      */
  }

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "split" => split(inputFilepath)
      case "extract" => extract(inputFilepath, cmd.getArgList.get(1))
      case "areas" => resolveAreas(inputFilepath, cmd.getArgList.get(1))
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

  def resolveAreas(inputFilepath: String, outputFilepath: String): Unit = {
    def all(entity: Entity): Boolean  = true

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

    def exportArea(area: Area, output: OutputStream): Unit = {
      val latitudes = mutable.ListBuffer[Double]()
      val longitudes = mutable.ListBuffer[Double]()
      val pointCount = area.polygon.getPointCount - 1
      (0 to pointCount).map { i =>
        val p = area.polygon.getPoint(i)
        latitudes.+=(p.getX)
        longitudes.+=(p.getY)
      }

      val outputArea = OutputArea(id = Some(area.id), osmId = area.osmId, name = Some(area.name), parent = None, latitudes = latitudes, longitudes = longitudes)
      outputArea.writeDelimitedTo(output)
    }

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

    val earthPolygon = makePolygon((-180, 90),(180, -90))
    val earth = Area(0, "Earth", earthPolygon, boundingBoxFor(earthPolygon))
    exportArea(earth, oos)

    logger.info("Resolving areas for " + relationsToResolve.size + " relations")
    areaResolver.resolveAreas(relationsToResolve, relations, wayResolver, nodeResolver, callback)

    logger.info("Resolving areas for " + waysToResolve.size + " ways")
    areaResolver.resolveAreas(waysToResolve, relations, wayResolver, nodeResolver, callback)  // TODO why are two sets of ways in scope?

    oos.flush()
    oos.close
    logger.info("Dumped areas to file: " + outputFilepath)
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
    var areas = Seq[Area]()
    val fileInputStream = new BufferedInputStream(new FileInputStream(inputFilename))

    val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
    var ok = true
    while (ok) {
      counter.withProgress {
        val outputArea = OutputArea.parseDelimitedFrom(fileInputStream)
        outputArea.map { oa =>
          val area = outputAreaToArea(oa)
          areas = areas.+:(area)
        }
        ok = outputArea.nonEmpty
      }
    }

    fileInputStream.close
    logger.info("Read " + areas.size + " areas")
    areas
  }

  private def outputAreaToArea(oa: OutputArea): Area = {
    val points: Seq[(Double, Double)] = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    val p = areaForPoints(points).get
    Area(id = oa.id.get, name = oa.name.get, polygon = p, boundingBox = boundingBoxFor(p), osmId = oa.osmId) // TODO Naked gets
  }

}
