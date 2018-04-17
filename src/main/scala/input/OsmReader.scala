package input

import java.io.FileInputStream

import crosby.binary.osmosis.OsmosisReader
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.task.v0_6.Sink

class OsmReader(extractFilePath: String, sink: Sink) extends Logging {

  def read = {
    val inputStream = new FileInputStream(extractFilePath)
    val reader = new OsmosisReader(inputStream)
    reader.setSink(sink)
    reader.run()
    logger.info("Closing read")
    inputStream.close
  }

}
