package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.Entity

trait EntityOsmId {

  def osmIdFor(e: Entity): String = {
    e.getId.toString + e.getType.toString.take(1)
  }

}
