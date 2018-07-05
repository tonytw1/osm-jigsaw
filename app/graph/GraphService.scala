package graph

import java.net.URL
import javax.inject.Inject

import play.api.Configuration
import tags.TagService

class GraphService @Inject()(configuration: Configuration, tagService: TagService) {

  val areasFile = new URL(configuration.getString("areas.url").get)
  val graphFile = new URL(configuration.getString("graph.url").get)

  val head: GraphNode = new GraphReader().loadGraph(areasFile, graphFile)

  def tagsFor(osmId: String): Option[Seq[(String, String)]] = {
    tagService.tagsFor(osmId)
  }

}
