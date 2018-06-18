package graph

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject

import play.api.{Configuration, Logger}

class GraphService @Inject()(configuration: Configuration) {

  var head: Area = null

  {
    val file = new URL(configuration.getString("graph.url").get)
    Logger.info("Loading graph from: " + file)
    head = new GraphReader().loadGraph(new BufferedInputStream(file.openStream()))
  }

}
