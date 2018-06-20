package resolving

import input.SinkRunner
import org.openstreetmap.osmosis.core.domain.v0_6.Entity

import scala.collection.mutable

trait LoadTestEntities {

  private def all(entity: Entity): Boolean = true

  def loadEntities(filepath: String, predicate: Entity => Boolean = all): Seq[Entity] = {
    var found = mutable.Buffer[Entity]()
    def addToBuffer(entity: Entity) = found = found.+=(entity)
    new SinkRunner(filepath, predicate, addToBuffer).run
    found
  }

}
