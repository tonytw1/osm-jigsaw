import java.net.URL

import graph.GraphReader
import model.GraphNode
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging

object Main extends Logging {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)

    val areasFile = new URL(cmd.getArgList.get(0)) // TODO validation required
    val graphFile = new URL(cmd.getArgList.get(1)) // TODO validation required

    val head = new GraphReader().loadGraph(areasFile, graphFile)

    descend(head, Seq.empty)

    logger.info("Done")
  }

  def descend(node: GraphNode, parents: Seq[GraphNode]): Unit = {
    if (node.children.nonEmpty) {
      node.children.foreach(c => descend(c, parents :+ node))

    } else {
      println ((parents :+ node).map(_.area.id.toString).mkString(" / "))
    }
  }

}
