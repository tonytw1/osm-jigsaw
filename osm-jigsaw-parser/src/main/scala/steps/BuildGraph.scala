package steps

import areas.AreaComparison
import graphing.{GraphBuilder, GraphWriter}
import input.AreaReading
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import output.OutputFiles
import progress.ProgressCounter

import java.io.{BufferedOutputStream, FileOutputStream}

class BuildGraph extends OutputFiles with AreaReading with Segmenting with AreaComparison with Logging {

  def buildGraph(extractName: String) = {
    val areas = readAreasFromPbfFile(areasFilePath(extractName))

    logger.info("Building graph")

    val headArea = Area(-1L, null, (0,0,0,0), area = 0, convexHull = None)
    val graph = new GraphBuilder().buildGraph(headArea, areas)
    writeGraph(graph, new FileOutputStream(graphFile(extractName)))
    logger.info("Done")
  }

  private def writeGraph(head: GraphNode, outputStream: FileOutputStream) = {
    logger.info("Writing graph to disk")
    val output = new BufferedOutputStream(outputStream)
    val counter = new ProgressCounter(10000)
    new GraphWriter().export(head, output, None, counter)
    output.flush()
    output.close()
  }

}
