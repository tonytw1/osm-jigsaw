import java.io.File

import graph.GraphReader
import org.specs2.mutable._

class ApplicationSpec extends Specification {

  "build graph from pbf" in {
    new GraphReader().loadGraph(new File("great-britain-latest.graph.pbf"))
    1 must equalTo(1)
  }

}
