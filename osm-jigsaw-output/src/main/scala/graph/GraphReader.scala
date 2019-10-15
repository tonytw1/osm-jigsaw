package graph

import java.io.BufferedInputStream
import java.net.URL

import model.{Area, GraphNode, OsmIdParsing, Point}
import org.apache.logging.log4j.scala.Logging
import outputarea.OutputArea
import outputgraphnode.OutputGraphNode
import progress.ProgressCounter

import _root_.scala.collection.mutable

class GraphReader extends OsmIdParsing with Logging {

  def loadGraph(areasFile: URL, graphFile: URL): GraphNode = {

    def loadAreas(areasFile: URL): Map[Long, Area] = {

      def outputAreaToArea(oa: OutputArea): Option[Area] = {
        oa.id.flatMap { id =>
          oa.area.map { a =>
            val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
            Area(id = id, points = points, osmIds = oa.osmIds.map(toOsmId), area = a)
          }
        }
      }

      val areasMap = mutable.Map[Long, Area]()
      val input = new BufferedInputStream(areasFile.openStream())
      val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val added = OutputArea.parseDelimitedFrom(input).flatMap { oa =>
            outputAreaToArea(oa).map { area =>
              areasMap += area.id -> area
              area
            }
          }
          ok = added.nonEmpty
        }
      }
      logger.info("Mapped areas: " + areasMap.size)
      input.close()
      areasMap.toMap
    }

    try {
      val areas = loadAreas(areasFile)

      def toGraphNode(ogn: OutputGraphNode): Option[GraphNode] = {
        ogn.area.flatMap { areaId =>
          areas.get(areaId).map { area =>
            GraphNode(area = area)
          }
        }
      }

      val input = new BufferedInputStream(graphFile.openStream())
      val stack = mutable.Stack[GraphNode]()

      val counterSecond = new ProgressCounter(step = 10000, label = Some("Building graph"))
      var ok = true
      while (ok) {
        counterSecond.withProgress {
          ok = OutputGraphNode.parseDelimitedFrom(input).flatMap { oa =>
            toGraphNode(oa).map { node =>
              val insertInto = if (stack.nonEmpty) {
                var insertInto = stack.pop
                while (!oa.parent.contains(insertInto.area.id)) {
                  insertInto = stack.pop
                }
                insertInto.children += node
                insertInto
              } else {
                node
              }

              stack.push(insertInto)
              stack.push(node)
              node
            }
          }.nonEmpty
        }
      }
      input.close()

      logger.info("Finished reading")
      stack.last

    } catch {
      case e: Exception =>
        logger.error("Error: " + e)
        throw e
    }
  }

}
