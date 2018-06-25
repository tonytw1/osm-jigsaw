package graph

import java.net.URL
import javax.inject.Inject

import play.api.Configuration

class GraphService @Inject()(configuration: Configuration) {

  val areasFile = new URL(configuration.getString("areas.url").get)
  val graphFile = new URL(configuration.getString("graph.url").get)

  val head: GraphNode = new GraphReader().loadGraph(areasFile, graphFile)

}
