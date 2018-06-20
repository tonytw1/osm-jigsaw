package graph

import java.io.BufferedInputStream
import java.net.URL

import org.specs2.mutable._

class GraphReaderSpec extends Specification {

  "build graph from pbf" in {
    val file = new URL(" http://10.0.45.22:32680/osm/great-britain-180614.graph.pbf")

    val head = new GraphReader().loadGraph(file)

    head.name must equalTo(Some("Earth"))
    head.children.size must equalTo(104)
  }

}
