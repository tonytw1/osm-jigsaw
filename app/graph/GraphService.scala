package graph

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject

import play.api.{Configuration, Logger}

class GraphService @Inject()(configuration: Configuration) {

  val file = new URL(configuration.getString("graph.url").get)
  val head: Area = new GraphReader().loadGraph(file)

}
