package resolving

import model.JoinedWay

trait WayJoining {

  def nodesFor(joinedWays: Seq[JoinedWay]): Seq[Long] = {
    val outerNodeIds = joinedWays.map { jw =>
      if (jw.reverse) {
        jw.way.nodes.reverse
      } else {
        jw.way.nodes
      }
    }.flatten
    outerNodeIds
  }

}
