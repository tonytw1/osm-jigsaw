package steps

import areas.AreaComparison
import graphing.GraphBuilder
import input.AreaReading
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import output.{GraphWriting, OutputFiles}
import progress.ProgressCounter

import java.io.{BufferedOutputStream, FileOutputStream}

class BuildGraph extends OutputFiles with AreaReading with AreaComparison with GraphWriting with Logging {

  def buildGraph(extractName: String) = {
    val areas = readAreasFromPbfFile(areasFilePath(extractName))

    logger.info("Building graph")

    val headArea = Area(-1L, null, (0, 0, 0, 0), area = 0)
    val graph = new GraphBuilder().buildGraph(headArea, areas)
    writeGraph(graph, new FileOutputStream(graphFile(extractName)))
    logger.info("Done")
  }

  private def writeGraph(head: GraphNode, outputStream: FileOutputStream) = {
    logger.info("Writing graph to disk")
    val output = new BufferedOutputStream(outputStream)
    outputGraph(head, output)
    output.flush()
    output.close()
  }

}
