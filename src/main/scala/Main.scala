import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import graphing.GraphBuilder
import input.{RelationExtractor, SinkRunner}
import model.{Area, GraphNode}
import org.apache.commons.cli._
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OsmWriter
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

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilepath, allAdminBoundaries)

    val relations = extractedRelationsWithComponents._1
    val ways = extractedRelationsWithComponents._2
    val nodes = extractedRelationsWithComponents._3

    new OsmWriter(outputFilepath).write(relations.toSeq ++ ways.toSeq ++ nodes.toSeq)
    println("Dumped selected relations and resolved components to: " + outputFilepath)
  }


  def resolveAreas(inputFilepath: String, outputFilepath: String): Unit = {
    def all(entity: Entity): Boolean  = true

    val allFound = mutable.Buffer[Entity]()
    def addToFound(entity: Entity) = allFound.+=(entity)
    new SinkRunner(inputFilepath, all, addToFound).run

    val relations: Set[Relation] = allFound.flatMap { e =>
      e match {
        case r: Relation => Some(r)
        case _ => None
      }
    }.toSet

    val ways: Map[Long, Way] = allFound.flatMap { e =>
      e match {
        case w: Way => Some(w)
        case _ => None
      }
    }.map { i =>
      (i.getId, i)
    }.toMap

    val nodes = allFound.flatMap { e =>
      e match {
        case n: Node => Some(n)
        case _ => None
      }
    }.map { i =>
      (i.getId, i)
    }.toMap

    val allRelations = relations.map( r => (r.getId, r)).toMap  // TODO Does this contain all of the subrelations?
    println("Found " + relations.size + " relations to process")

    println("Resolving areas")
    val relationResolver = new RelationResolver()
    val areas = relationResolver.resolve(relations, allRelations, ways, nodes)
    println("Produced " + areas.size + " relation shapes")

    val oos = new ObjectOutputStream(new FileOutputStream(outputFilepath))
    oos.writeObject(areas)
    oos.close

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
