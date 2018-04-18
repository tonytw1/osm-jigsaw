package resolving

import model.Way

class WayResolver(ways: Map[Long, Way]) {

  def get(wayId: Long): Option[Way] = {
    ways.get(wayId)
  }

}
