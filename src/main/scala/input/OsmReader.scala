package input

import java.io.FileInputStream

import crosby.binary.osmosis.OsmosisReader
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

class OsmReader(extractFilePath: String) {

  def read(predicate: Entity => Boolean): Seq[Entity] = {
    val inputStream = new FileInputStream(extractFilePath)
    val sink = new OsmSink(predicate)
    val reader = new OsmosisReader(inputStream)
    reader.setSink(sink)
    reader.run()
    inputStream.close
    sink.found
  }.toSeq

}
