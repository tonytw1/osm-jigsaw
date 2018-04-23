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
import resolving.{AreaResolver, MapDBNodeResolver, MapDBWayResolver}

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap

object Main extends EntityRendering with Logging {

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
      case "dump" => dumpGraph(inputFilepath)
      case "export" => exportGraph(inputFilepath, cmd.getArgList.get(1))
      case "verify" => verifyGraph(inputFilepath)
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
    val oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFilepath)))

    def callback(newAreas: Seq[Area]): Unit = {
      newAreas.foreach { a =>
        oos.writeUnshared(a)
      }
      oos.reset()
    }

    logger.info("Filtering relations to resolve")
    val relationsToResolve: Iterable[Relation] = relations.values.filter(e => entitiesToGraph(e))

    val areaResolver = new AreaResolver()
    val wayResolver = new MapDBWayResolver(inputFilepath + ".ways.vol")
    val nodeResolver = new MapDBNodeResolver(inputFilepath + ".nodes.vol")

    logger.info("Resolving areas for " + relationsToResolve.size + " relations")
    areaResolver.resolveAreas(relationsToResolve, relations, wayResolver, nodeResolver, callback)

    logger.info("Resolving areas for " + waysToResolve.size + " ways")
    areaResolver.resolveAreas(waysToResolve, relations, wayResolver, nodeResolver, callback)  // TODO why are two sets of ways in scope?

    oos.flush()
    oos.close
    logger.info("Dumped areas to file: " + outputFilepath)
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    logger.info("Reading areas")
    var areas = Seq[Area]()
    val fileInputStream = new BufferedInputStream(new FileInputStream(inputFilename))
    val ois = new ObjectInputStream(fileInputStream)
    try {
      while (true) {
        areas = areas.+:(ois.readObject().asInstanceOf[Area])
      }
    } catch {
      case e: EOFException => logger.info("Reached end of file")
    }
    ois.close
    logger.info("Read " + areas.size + " areas")

    logger.info("Building graph")
    val head = new GraphBuilder().buildGraph(areas)

    logger.info("Writing graph to disk")
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilename))
    oos.writeObject(head)
    oos.close
    logger.info("Dumped graph to file: " + outputFilename)
  }

  def dumpGraph(inputFilename: String) = {
    logger.info("Opening graph file: " + inputFilename)
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    logger.info("Reading object")
    val graph = ois.readObject.asInstanceOf[GraphNode]
    logger.info("Closing")
    ois.close

    logger.info("Beginning dump")
    new GraphReader().dump(graph)
    logger.info("Done")
  }

  def exportGraph(inputFilename: String, outputFilename: String) = {
    logger.info("Opening graph file: " + inputFilename)
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    logger.info("Reading object")
    val graph = ois.readObject.asInstanceOf[GraphNode]
    logger.info("Closing")
    ois.close

    val output = new BufferedOutputStream(new FileOutputStream(outputFilename))
    val counter = new ProgressCounter(100000)

    logger.info("Export dump")
    new GraphReader().export(graph, output, None, counter)

    output.flush()
    output.close()
    logger.info("Done")
  }

  def verifyGraph(inputFilename: String) = {
    logger.info("Opening graph pbf file: " + inputFilename)
    val is = new BufferedInputStream(new FileInputStream(inputFilename))

    var ok = true
    while(ok) {
      val area = OutputArea.parseDelimitedFrom(is)
      println(area)
      ok = area.nonEmpty
    }

    logger.info("Closing")
    is.close
  }


}
