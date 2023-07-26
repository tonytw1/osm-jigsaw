package steps

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains}
import graph.GraphReader
import input.AreaReading
import model.{Area, FlippedGraphNode, GraphNode}
import org.apache.logging.log4j.scala.Logging
import output.{AreaWriting, GraphWriting, OutputFiles}
import outputtagging.OutputTagging
import progress.ProgressCounter
import tiles.TileGenerator

import java.io._
import scala.collection.mutable

class Tiling extends AreaReading with AreaComparison with OutputFiles with AreaWriting with GraphWriting with Logging {

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

            val unzippedTags = tags.unzip
            val keys = unzippedTags._1
            val values = unzippedTags._2

            OutputTagging(osmId = Some(osmId), keys = keys, values = values).writeDelimitedTo(tagsOutput)
          }
          tagsOutput.flush()
          tagsOutput.close()
        }

        Operator.deaccelerateGeometry(tilePolygon)
      }
    }
  }

}
