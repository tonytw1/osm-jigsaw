package input

import java.io.FileInputStream

import crosby.binary.osmosis.OsmosisReader
import org.openstreetmap.osmosis.core.task.v0_6.Sink

class OsmReader(extractFilePath: String, sink: Sink) {

  def read = {
    val inputStream = new FileInputStream(extractFilePath)
    val reader = new OsmosisReader(inputStream)
    reader.setSink(sink)
    reader.run()
    println("Closing read")
    inputStream.close
  }

}
