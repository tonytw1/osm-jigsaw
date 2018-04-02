import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import graphing.{GraphBuilder, GraphReader}
import input.{RelationExtractor, SinkRunner}
import model.{Area, GraphNode}
import org.apache.commons.cli._
import org.openstreetmap.osmosis.core.domain.v0_6._
import resolving.RelationResolver

import scala.collection.JavaConverters._
import scala.collection.immutable.LongMap

object Main {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

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
      case _ => println("Unknown step") // TODO exit code
    }
  }

  def extract(inputFilepath: String, outputFilepath: String) {
    println("Extracting relations and their resolved components from " + inputFilepath + " into " + outputFilepath)

    // Predicate to describe the top level relations we wish to extract
    def allAdminBoundaries(entity: Entity): Boolean = {
      val tags = entity.getTags.asScala
      val isAdminLevel = tags.exists(t => t.getKey == "admin_level")
      val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
      val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
      entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag // TODO ensure type is tested before more expensive tag operations
    }

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilepath, allAdminBoundaries, outputFilepath)

    println("Done")
  }

  def extractRelations(inputFilepath: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    println(relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilepath, selectedRelations, outputFilepath)

    println("Done")
  }

  def resolveAreas(inputFilepath: String, outputFilepath: String): Unit = {
    def all(entity: Entity): Boolean  = true

    var relations = LongMap[Relation]()
    var ways = LongMap[Seq[Long]]()
    var nodes = LongMap[(Double, Double)]()

    def addToFound(entity: Entity) = {
      entity match {
        case r: Relation => relations = relations + (r.getId -> r)
        case w: Way => ways = ways + (w.getId -> w.getWayNodes.asScala.map(wn => wn.getNodeId))
        case n: Node => nodes = nodes + (n.getId -> (n.getLatitude, n.getLongitude))
        case _ =>
      }
    }

    println("Loading entities")
    new SinkRunner(inputFilepath, all, addToFound).run
    println("Finished loading entities")

    println("Found " + relations.size + " relations to process")

    println("Resolving areas")
    val relationResolver = new RelationResolver()
    val areas = relationResolver.resolveAreas(relations.values.toSet, relations, ways, nodes)
    println("Produced " + areas.size + " relation shapes")

    println("Dumping areas to file")
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilepath))
    oos.writeObject(areas)
    oos.close

    println("Dumped areas to file: " + outputFilepath)
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    val areas = ois.readObject.asInstanceOf[Set[Area]] // TODO order?
    ois.close

    val head = new GraphBuilder().buildGraph(areas.toSeq)

    // Dump graph to disk
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilename))
    oos.writeObject(head)
    oos.close
    println("Dumped graph to file: " + outputFilename)
  }

  def dumpGraph(inputFilename: String) = {
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    val graph = ois.readObject.asInstanceOf[GraphNode]
    ois.close

    new GraphReader().dump(graph)
  }

}
