import graphing.EntitiesToGraph
import input._
import model.EntityRendering
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OutputFiles
import outputgraphnode.OutputGraphNode
import outputgraphnodev2.OutputGraphNodeV2
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import progress.ProgressCounter
import resolving._
import steps._

import java.io._
import scala.collection.mutable

object Main extends EntityRendering with Logging with PolygonBuilding
  with ProtocolbufferReading with EntityOsmId
  with Extracts with WorkingFiles with EntitiesToGraph with AreaReading with OutputFiles {

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
      case "extract" => new ExtractEntities().extract(inputFilepath)
      case "namednodes" => extractNamedNodes(inputFilepath, cmd.getArgList.get(1))
      case "areaways" => new ExtractAreas().resolveAreaWays(inputFilepath)
      case "areastats" => areaStats(inputFilepath)
      case "areas" => new RenderAndDeduplicateAreas().resolveAreas(inputFilepath)
      case "graph" => new BuildGraph().buildGraph(inputFilepath)
      case "tags" => new ExtractAreaTags().tags(inputFilepath)
      case "rels" => {
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      }
      case "flip" =>
        flipGraph(inputFilepath)
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

  def extractRelations(extractName: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    logger.info("Extracting specific relations: " + relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    new RelationExtractor().extract(extractName, selectedRelations, outputFilepath)

    logger.info("Done")
  }

  case class FlippedGraphNode(id: Long, children: mutable.Set[FlippedGraphNode]) {
    override def hashCode(): Int = id.hashCode()
  }

  def flipGraph(extractName: String): Unit = {
    val input = new BufferedInputStream(new FileInputStream(new File(graphFile(extractName))))
    val rootId = -1L

    var ok = true
    // Remap out nodes with parents formatted graph into nodes with children
    // There is likely massive duplication of nodes in the existing format which is hurting on memory
    val nodes = mutable.Map[Long, FlippedGraphNode]()

    val progressMessage: (Long, scala.Option[Long], Long, Double) => String = (i: Long, total: scala.Option[Long], delta: Long, rate: Double) => {
        i + " / Unique nodes: " + nodes.size
    }

    var root: FlippedGraphNode = null
    val counterSecond = new ProgressCounter(step = 100, label = Some("Flipping graph"))
    while (ok) {
      counterSecond.withProgress({
        val maybeNode = OutputGraphNode.parseDelimitedFrom(input)
        ok = maybeNode.nonEmpty
        maybeNode.foreach { o =>
          for {
            area <- o.area
          } yield {
            // Get of create this node
            val node = nodes.getOrElseUpdate(area, FlippedGraphNode(area, mutable.Set[FlippedGraphNode]()))
            nodes.put(area, node)

            val parentId = o.parent.getOrElse(rootId)
            // Find or create the node for out parent
            val parentNode = nodes.getOrElseUpdate(parentId, {
              val newParent = FlippedGraphNode(parentId, mutable.Set[FlippedGraphNode]())
              nodes.put(parentId, newParent)
              newParent
            })

            if (node.id != rootId) {
              parentNode.children.add(node)
            }

            // Latch the root node when we see it go past
            if (area == rootId) {
              root = node
            }
          }
        }
      }, progressMessage)
    }
    logger.info("Root: " + root.id + " -> " + root.children.size)

    logger.info("Writing out flipped graph")
    val output = new BufferedOutputStream(new FileOutputStream(new File(graphV2File(extractName))))

    // Now we can write out the flipped graph
    // If we DFS write leaf nodes first then all children will have already been encountered by the time they are read.
    // Given our leaf first ordering, if a node appears more than once we can skip it; the reader will have already encountered it and it's children
    val persisted = mutable.Set[Long]()

    def visit(node: FlippedGraphNode): Unit = {
      if (!persisted.contains(node.id)) {
        node.children.foreach { c =>
          visit(c)
        }
        persisted.add(node.id)
        new OutputGraphNodeV2(node.id, node.children.map(_.id).toSeq).writeDelimitedTo(output)
      }
    }

    visit(root)
    output.flush()
    output.close()

    logger.info("Done")
  }

}
