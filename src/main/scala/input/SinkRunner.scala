package input

import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

class SinkRunner(filepath: String, predicate: Entity => Boolean, callback: Entity => Unit) {

  def run = {
    val sink = new OsmEntitySink(predicate, callback)
    val reader = new OsmReader(filepath, sink)
    reader.read
  }

}
