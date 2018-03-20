package input

import org.scalatest.FlatSpec
import java.io.FileInputStream

class OsmSinkSpec extends FlatSpec {

  "osm sink" should "read pdf file" in {
    val inputStream = new FileInputStream("great-britain-latest.osm.pbf")

    import crosby.binary.osmosis.OsmosisReader
    val reader = new OsmosisReader(inputStream)

    val sink = new OsmSink()

    reader.setSink(sink)
    reader.run()

    inputStream.close
    succeed
  }

}
