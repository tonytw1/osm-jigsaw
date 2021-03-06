package input

import java.io.InputStream

import com.google.common.io.CountingInputStream
import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

class SinkRunner(inputStream: InputStream, predicate: Entity => Boolean, callback: Entity => Unit) {

  val input = new CountingInputStream(inputStream) // TODO buffered?

  def run = {
    new OsmReader(input, new OsmEntitySink(predicate, callback)).read
  }

  def currentPosition: Long = input.getCount

}
