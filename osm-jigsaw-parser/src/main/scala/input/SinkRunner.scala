package input

import java.io.FileInputStream

import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

class SinkRunner(filepath: String, predicate: Entity => Boolean, callback: Entity => Unit) {

  val file = new FileInputStream(filepath)  // TODO buffered?

  def run = {
    val reader = new OsmReader(file, new OsmEntitySink(predicate, callback))
    reader.read
  }

  def currentPosition: Long = {
    file.getChannel.position()
  }

}
