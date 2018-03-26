import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import graphing.GraphBuilder
import input.{RelationExtractor, SinkRunner}
import model.{Area, GraphNode}
import org.apache.commons.cli._
import org.openstreetmap.osmosis.core.domain.v0_6._
import resolving.RelationResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

object Main {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required
    val outputFilepath = cmd.getArgList.get(1) // TODO validatio required

    step match {
      case "extract" =>
        extract(inputFilepath, outputFilepath)

      case "areas" =>
        resolveAreas(inputFilepath, outputFilepath)

      case "graph" =>
        buildGraph(inputFilepath, outputFilepath)

      case _ => {
        println("Unknown step")
        // TODO exit code
      }
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

  def resolveAreas(inputFilepath: String, outputFilepath: String): Unit = {
    def all(entity: Entity): Boolean  = true

    val relations = mutable.Buffer[Relation]()
    val ways = mutable.Buffer[Way]()
    val nodes = mutable.Buffer[(Long, Double, Double)]()

    def addToFound(entity: Entity) = {
      entity match {
        case r: Relation => relations.+=(r)
        case w: Way => ways.+=(w)
        case n: Node => nodes.+=((n.getId, n.getLatitude, n.getLongitude))
        case _ =>
      }
    }

    println("Loading entities")
    new SinkRunner(inputFilepath, all, addToFound).run
    println("Finished loading entities")

    println("Found " + relations.size + " relations to process")

    println("Building relations lookup map")
    val relationsMap = relations.map( r => (r.getId, r)).toMap  // TODO Does this contain all of the subrelations?
    println("Building ways lookup map")
    val waysMap = ways.map(w => w.getId -> w).toMap
    println("Building nodes lookup map")
    val nodesMap = nodes.map(n => n._1 -> n).toMap

    println("Resolving areas")
    val relationResolver = new RelationResolver()
    val areas = relationResolver.resolve(relations.toSet, relationsMap, waysMap, nodesMap)
    println("Produced " + areas.size + " relation shapes")

    println("Dumping areas to file")
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilepath))
    oos.writeObject(areas)
    oos.close

    areas.map { a =>
      println(a.boundingBox)
    }

    println("Dumped areas to file: " + outputFilepath)
  }

  def buildGraph(inputFilename: String, outputFilename: String) = {
    val ois = new ObjectInputStream(new FileInputStream(inputFilename))
    val areas: Set[Area] = ois.readObject.asInstanceOf[Set[Area]] // TODO order?
    ois.close

    val head = new GraphBuilder().buildGraph(areas.toSeq)

    def dump(node: GraphNode, soFar: String): Unit = {
      val path = soFar + " / " + node.area.name
      if (node.children.nonEmpty) {
        node.children.map { c =>
          dump(c, path)
        }
      } else {
        println(path)
      }
    }

    println("_________________")
    dump(head, "")

    // Dump graph to disk
    val oos = new ObjectOutputStream(new FileOutputStream(outputFilename))
    oos.writeObject(head)
    oos.close

    println("Dumped graph to file: " + outputFilename)
  }

}
