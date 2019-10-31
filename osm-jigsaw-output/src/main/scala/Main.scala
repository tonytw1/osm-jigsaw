import java.io.File

import graph.GraphReader
import model.{GraphNode, OsmId}
import naming.NaiveNamingService
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import tags.TagService

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends Logging {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)

    val areasFile = new File(cmd.getArgList.get(0)) // TODO validation required
    val graphFile = new File(cmd.getArgList.get(1)) // TODO validation required
    val tagsFile = new File(cmd.getArgList.get(2)) // TODO validation required

    val head = new GraphReader().loadGraph(areasFile.toURI.toURL, graphFile.toURI.toURL)

    // Traverse the tree to discover all possible paths
    val traces = ListBuffer[Seq[GraphNode]]()
    descend(head, Seq.empty, traces)

    // Sort by leaf node ids to group for naming.
    val sorted = traces.sortBy(_.head.area.id)


    val tagsService = new TagService(tagsFile.toURI.toURL)
    val namingService = new NaiveNamingService(tagsService)

    var current: Long = 0
    var group: ListBuffer[Seq[GraphNode]] = ListBuffer.empty
    sorted.foreach{ i =>
      if (current > 0 && current != i.head.area.id) {
        val x: mutable.Seq[Seq[(Seq[OsmId], Double)]] = group.map { i =>
          i.map { j =>
            (j.area.osmIds, j.area.area)
          }.reverse
        }

        println(namingService.nameFor(x))
        group = ListBuffer.empty

      }
      current = i.head.area.id
      group = group += i

    }

    logger.info("Done: " + traces.size)
  }

  def descend(node: GraphNode, parents: Seq[GraphNode], traces: ListBuffer[Seq[GraphNode]]): Unit = {
    if (node.children.nonEmpty) {
      node.children.foreach(c => descend(c, parents :+ node, traces))

    } else {
      val reverse = (parents :+ node).reverse
      traces += reverse
    }
  }

}
