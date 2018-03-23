package output

import java.io.FileOutputStream

import crosby.binary.osmosis.OsmosisSerializer
import org.openstreetmap.osmosis.core.container.v0_6.{NodeContainerFactory, RelationContainerFactory, WayContainerFactory}
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Node, Relation, Way}
import org.openstreetmap.osmosis.osmbinary.file.BlockOutputStream

class OsmWriter(outputFilePath: String) {

  val nodeContainerFactory = new NodeContainerFactory()
  val relationContainerFactory = new RelationContainerFactory()
  val wayContainerFactory = new WayContainerFactory()

  def write(entities: Seq[Entity]) = {
    val out = new FileOutputStream(outputFilePath)
    var blockOutputStream: BlockOutputStream = new BlockOutputStream(out)

    val serializer = new OsmosisSerializer(blockOutputStream)

    entities.map { e =>
      e match {
        case n: Node => serializer.process(nodeContainerFactory.createContainer(n))
        case r: Relation => serializer.process(relationContainerFactory.createContainer(r))
        case w: Way => serializer.process(wayContainerFactory.createContainer(w))
      }
    }

    serializer.complete()
    serializer.close()
  }

}
