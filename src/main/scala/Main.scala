import java.io._

import graphing.{GraphBuilder, GraphReader}
import input.{RelationExtractor, SinkRunner}
import model.{Area, EntityRendering, GraphNode}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import resolving.AreaResolver

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap

object Main extends EntityRendering with Logging {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def entitiesToGraph(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevel = tags.exists(t => t.getKey == "admin_level")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")

    val isLeisurePark = tags.exists(t => tags.exists(t => t.getKey == "leisure"  && t.getValue == "park"))
    val isIsland = tags.exists(t => tags.exists(t => t.getKey == "place"  && t.getValue == "island"))
    val isNationalPark = tags.exists(t => tags.exists(t => t.getKey == "boundary"  && t.getValue == "national_park"))

    (entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag) ||
      ((entity.getType == EntityType.Relation || entity.getType == EntityType.Way && entity.asInstanceOf[Way].isClosed) && isLeisurePark || isNationalPark || isIsland)
  }

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "extract" => extract(inputFilepath, cmd.getArgList.get(1))
      case "areas" => resolveAreas(inputFilepath, cmd.getArgList.get(1))
      case "graph" => buildGraph(inputFilepath, cmd.getArgList.get(1))
      case "dump" => dumpGraph(inputFilepath)
      case "rels" => {
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      }
      case _ => logger.info("Unknown step") // TODO exit code
    }
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
    var ways = LongMap[Way]()
    var modelWays = LongMap[model.Way]()
    var nodes = LongMap[(Double, Double)]()

    def addToFound(entity: Entity) = {
      entity match {
        case r: Relation => relations = relations + (r.getId -> r)
        case w: Way => {
          ways = ways + (w.getId -> w)
          modelWays = modelWays + (w.getId -> model.Way(w.getId, nameFor(w), w.getWayNodes.asScala.map(wn => wn.getNodeId)))
        }
        case n: Node => nodes = nodes + (n.getId -> (n.getLatitude, n.getLongitude))
        case _ =>
      }
    }

    logger.info("Loading entities")
    new SinkRunner(inputFilepath, all, addToFound).run
    logger.info("Finished loading entities")

    logger.info("Found " + relations.size + " relations to process")

    logger.info("Resolving areas")

    val relationsToResolve: Set[Entity] = (relations.values.toSeq).filter(e => entitiesToGraph(e)).toSet
    val waysToResolve: Set[Entity] = (ways.values.toSeq).filter(e => entitiesToGraph(e)).toSet

    val entitiesToResolve = relationsToResolve ++ waysToResolve

    val areaResolver = new AreaResolver()

    val oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFilepath)))

    def callback(newAreas: Seq[Area]): Unit = {
      newAreas.foreach(a => oos.writeObject(a))
    }

    areaResolver.resolveAreas(entitiesToResolve, relations, modelWays, nodes, callback)

    oos.close
    logger.info("Dumped areas to file: " + outputFilepath)
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    var areas = Seq[Area]()
    val fileInputStream = new FileInputStream(inputFilename)
    val ois = new ObjectInputStream(fileInputStream)
    while(fileInputStream.available > 0) {
      areas = areas :+ ois.readObject().asInstanceOf[Area]
    }
    ois.close

    val head = new GraphBuilder().buildGraph(areas.toSeq)

    // Dump graph to disk
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilename))
    oos.writeObject(head)
    oos.close
    logger.info("Dumped graph to file: " + outputFilename)
  }

  def dumpGraph(inputFilename: String) = {
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    val graph = ois.readObject.asInstanceOf[GraphNode]
    ois.close

    new GraphReader().dump(graph)
  }

}
