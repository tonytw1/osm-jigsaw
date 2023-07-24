import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains}
import graph.GraphReader
import graphing.EntitiesToGraph
import input._
import model.{Area, EntityRendering, GraphNode}
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import output.OutputFiles
import outputarea.OutputArea
import outputgraphnode.OutputGraphNode
import outputgraphnodev2.OutputGraphNodeV2
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import outputtagging.OutputTagging
import progress.ProgressCounter
import resolving._
import steps._
import tiles.TileGenerator

import java.io._
import scala.collection.mutable

object Main extends EntityRendering with Logging with PolygonBuilding
  with ProtocolbufferReading with EntityOsmId
  with Extracts with WorkingFiles with EntitiesToGraph with AreaReading with OutputFiles with AreaComparison {

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
      case "rels" =>
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      case "flip" => flipGraph(inputFilepath)
      case "tile" => tileGraph(inputFilepath)
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
    outputFlippedGraph(root, output)
    logger.info("Done")
  }

  def tileGraph(extractName: String): Unit = {
    // Read the entire graph into memory
    val areas = readAreasFromPbfFile(areasFilePath(extractName))

    val graphInput = new BufferedInputStream(new FileInputStream(new File(graphV2File(extractName))))
    val root = new GraphReader().loadGraph(graphInput, areas).get

    val tagsFile = new File(tagsFilePath(extractName))
    val tagsInput = new BufferedInputStream(new FileInputStream(tagsFile))
    val taggings = mutable.Map[String, Map[String, String]]()
    val tagsCount = new ProgressCounter(step = 10000, label = Some("Reading tags"))
    var ok = true
    while (ok) {
      tagsCount.withProgress {
        val outputTagging = OutputTagging.parseDelimitedFrom(tagsInput)
        outputTagging.foreach { ot =>
          val keys: Seq[String] = ot.keys
          val values: Seq[String] = ot.values
          val tuples: Map[String, String] = keys.zip(values).toMap
          taggings.put(ot.osmId.get, tuples)
        }
        ok = outputTagging.nonEmpty
      }
    }
    tagsInput.close()

    def exportArea(area: Area, output: OutputStream): Unit = { // TODO duplication
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

    // Geohash boundaries are a nice repeatable set of tiles with some course control over tile size
    // Generate some tile shapes
    val tiles = new TileGenerator().generateTiles(3)

    val counterTiles = new ProgressCounter(step = 10, label = Some("Filtering tile"))
    tiles.par.foreach { t =>
      counterTiles.withProgress {
        val nodes = mutable.Map[Long, FlippedGraphNode]()
        val tileTaggings = mutable.Map[String, Map[String, String]]()

        // For each tile filter walk the graph and filter for all areas which intersect the tile
        val topLeft = (t.boundingBox.getNorthEastCorner.getLatitude, t.boundingBox.getSouthWestCorner.getLongitude)
        val bottomRight = (t.boundingBox.getSouthWestCorner.getLatitude, t.boundingBox.getNorthEastCorner.getLongitude)
        val tilePolygon = makePolygonD(topLeft, bottomRight)
        OperatorContains.local().accelerateGeometry(tilePolygon, sr, GeometryAccelerationDegree.enumMedium)

        val tileArea = Area(-1, tilePolygon, boundingBoxFor(tilePolygon), area = 0.0)

        // Create a new graph root for this segment
        val newRoot = FlippedGraphNode(root.area.id, mutable.Set[FlippedGraphNode]())
        val tileAreas = mutable.Set[Area]()
        val visited = mutable.Set[Long]()

        def visit(node: GraphNode, appendTo: FlippedGraphNode): Unit = {
          // Check if we fit in the tile
          val intersectsWithTile = areasIntersect(node.area, tileArea)

          if (intersectsWithTile) {
            // This node belongs in this tiles graph
            // Add it to the new graph and append the area to this tile's areas
            val newNode = nodes.getOrElseUpdate(node.area.id, FlippedGraphNode(node.area.id, mutable.Set[FlippedGraphNode]()))
            appendTo.children.add(newNode)
            if (!visited.contains(node.area.id)) {
              tileAreas.add(node.area)
              // Capture the tags for this node
              val areasTags: mutable.Seq[(String, Map[String, String])] = node.area.osmIds.map { osmId =>
                (osmId, taggings.getOrElse(osmId, Map[String, String]()))
              }
              areasTags.foreach { kv: (String, Map[String, String]) =>
                tileTaggings.put(kv._1, kv._2)
              }
              visited.add(node.area.id)
              // Recurse down into children
              val children = node.children.toSeq
              children.foreach(c => visit(c, newNode))
            }
          }
        }

        val topLevel = root.children.toSeq
        topLevel.foreach { c =>
          visit(c, newRoot)
        }

        if (newRoot.children.nonEmpty) {
          val segmentGraphFile = new File(graphV2File(extractName, Some(t.geohash)))
          // Write this new graph to a new file
          val tileGraphOutput = new BufferedOutputStream(new FileOutputStream(segmentGraphFile))
          outputFlippedGraph(newRoot, tileGraphOutput)

          // Write out segmented areas file
          val tileAreasFile = new File(areasFilePath(extractName, Some(t.geohash)))
          val tileAreasOutput = new BufferedOutputStream(new FileOutputStream(tileAreasFile))
          tileAreas.foreach { a =>
            exportArea(a, tileAreasOutput)
          }
          tileAreasOutput.flush()
          tileGraphOutput.close()

          // Write out segment tags file
          val tagsOutputFilepath = tagsFilePath(extractName, Some(t.geohash))
          val tagsOutput = new BufferedOutputStream(new FileOutputStream(tagsOutputFilepath))
          tileTaggings.foreach { tagging: (String, Map[String, String]) =>
            val osmId = tagging._1
            val tags: Seq[(String, String)] = tagging._2.toSeq
            val keys = tags.map(_._1) // TODO unzip?
            val values = tags.map(_._2)
            OutputTagging(osmId = Some(osmId), keys = keys, values = values).writeDelimitedTo(tagsOutput)
          }
          tagsOutput.flush()
          tagsOutput.close()
        }

        Operator.deaccelerateGeometry(tilePolygon)
      }
    }
  }

  private def outputFlippedGraph(root: FlippedGraphNode, output: OutputStream): Unit = {
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
  }

}
