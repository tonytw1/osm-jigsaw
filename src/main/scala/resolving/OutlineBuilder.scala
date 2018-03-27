package resolving

import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.JavaConverters._
import scala.collection.mutable

class OutlineBuilder {

  val relationResolver = new RelationWayResolver()

  // Give a relation resolve it's outer to a seq of consecutively ordered points
  def outlineNodesFor(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, (Long, Double, Double)]): Seq[(Long, Double, Double)] = { // TODO handle missing Ways and nodes

    // Attempt to join up the ways (which may be out of order and facing in different directions) into a list consecutive nodes
    def joinWays(ways: Seq[Way]): Seq[Seq[Long]] = {
      val nonEmptyWayGroups = ways.map { w =>
        w.getWayNodes.asScala.map(wn => wn.getNodeId)
      }.filter(wg => wg.nonEmpty)

      if (nonEmptyWayGroups.nonEmpty) {
        val available = mutable.Set() ++ nonEmptyWayGroups // TODO what of relationship has multiple rings; available will not be fully consumed

        val first = available.head
        var joined = Seq(first)
        available.remove(first)

        while (available.nonEmpty && available.exists(wg => wg.head == joined.last.last || wg.last == joined.last.last)) {
          val next = available.find(wg => wg.head == joined.last.last || wg.last == joined.last.last).get
          if (next.head == joined.last.last) {
            joined = joined :+ next
          } else {
            joined = joined :+ next.reverse
          }
          available.remove(next)
        }
        joined

      } else {
        println("Relation has no non empty outers: " + r.getId)
        Seq()
      }
    }

    try {
      val rs = relationResolver.expandRelation(r, allRelations)
      val outerWays = relationResolver.resolveOuterWayIdsFor(rs, allRelations).map { wid =>
        ways.get(wid)
      }.flatten // TODO handle missing ways

      joinWays(outerWays).flatten.map { nid =>
        nodes.get(nid)
      }.flatten

    } catch {
      case e: Exception =>
        println("Failed to compose relation: " + r.getId, e.getMessage)
        throw (e)
    }
  }

}
