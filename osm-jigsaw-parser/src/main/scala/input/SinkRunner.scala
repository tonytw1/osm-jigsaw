package input

import java.io.FileInputStream

import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

class SinkRunner(filepath: String, predicate: Entity => Boolean, callback: Entity => Unit) {

  def run = {
    val inputStream = new FileInputStream(filepath)
    val reader = new OsmReader(inputStream, new OsmEntitySink(predicate, callback))
    reader.read
  }

}
