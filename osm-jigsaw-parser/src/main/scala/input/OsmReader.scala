package input

import java.io.InputStream

import crosby.binary.osmosis.OsmosisReader
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.task.v0_6.Sink

class OsmReader(inputStream: InputStream, sink: Sink) extends Logging {

  def read = {
    val reader = new OsmosisReader(inputStream)
    reader.setSink(sink)
    reader.run()
    logger.info("Closing read")
    inputStream.close
  }

}
