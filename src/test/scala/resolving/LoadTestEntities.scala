package resolving

import input.OsmReader
import input.sinks.OsmEntitySink
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

trait LoadTestEntities {

  def loadEntities(filepath: String, predicate: Entity => Boolean): Seq[Entity] = {
    var found = Seq[Entity]()

    def addToFoundWays(entity: Entity) = {
      found = found.+:(entity)
    }

    val sink = new OsmEntitySink(predicate, addToFoundWays)
    val reader = new OsmReader(filepath, sink)
    reader.read
    found.reverse
  }

}
