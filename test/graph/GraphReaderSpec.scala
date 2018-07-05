package graph

import java.io.BufferedInputStream
import java.net.URL

import org.specs2.mutable._
import outputtagging.OutputTagging
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReaderSpec extends Specification {

  /*
  "build graph from pbf" in {
    val areasFile = new URL("http://10.0.45.11:32680/osm/new-zealand-180619.areas.pbf")
    val graphFile = new URL("http://10.0.45.11:32680/osm/new-zealand-180619.graph.pbf")

    val head: GraphNode = new GraphReader().loadGraph(areasFile, graphFile)

    head.area.name must equalTo(Some("Earth"))
    head.children.size must equalTo(3)
  }
  */


}
