package resolving

import model.EntityRendering
import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.mutable

class OutlineBuilder extends EntityRendering {

  val relationExpander = new RelationExpander()
  val outerWayResolver = new OuterWayResolver()

  // Give a relation resolve it's outer to a seq of consecutively ordered points
  def outlineNodesFor(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Seq[Long]], nodes: Map[Long, (Double, Double)]): Seq[(String, Seq[(Double, Double)])] = { // TODO handle missing Ways and nodes

    // Attempt to join up the ways (which may be out of order and facing in different directions) into a list consecutive nodes
    def joinWays(ways: Seq[Seq[Long]]): Seq[Seq[Long]] = {
      val nonEmptyWayGroups = ways.filter(wg => wg.nonEmpty)

      if (nonEmptyWayGroups.nonEmpty) {
        val available = mutable.Set() ++ nonEmptyWayGroups // TODO what of relationship has multiple rings; available will not be fully consumed

        val first = available.head
        var joined = Seq(first)
        available.remove(first)

        def nextAttactment(wg: Seq[Long]): Boolean = wg.head == joined.last.last || wg.last == joined.last.last

        while (available.nonEmpty && available.exists(nextAttactment)) {
          val next = available.find(nextAttactment).get
          if (next.head == joined.last.last) {
            joined = joined :+ next
          } else {
            joined = joined :+ next.reverse
          }
          available.remove(next)
        }

        if (available.nonEmpty) {
          println("Relation " + r.getId + " had " + available.size + " available way groups after joining ways")
        }
        joined

      } else {
        println("Relation has no non empty outers: " + r.getId)
        Seq()
      }
    }

    try {
      val outerWays = outerWayResolver.resolveOuterWayIdsFor(r, allRelations).map { wid =>
        ways.get(wid)
      }.flatten // TODO handle missing ways

      val closedWays = outerWays.filter { w =>
        w.head == w.last
      }

      val waysToUse = if (outerWays.size > 1) {
        // TODO exclude closed ring ways
        val excludingClosedWays = outerWays.filterNot(w =>
          closedWays.contains(w)
        )
        excludingClosedWays

      } else {
        outerWays
      }

      val z = closedWays.map { cw =>
        cw.map(nid => nodes.get(nid)).flatten
        ("Closed way from " + render(r), cw.map(nid => nodes.get(nid)).flatten)  // TODO way name
      }


      val mainWay: (String, Seq[(Double, Double)]) = (render(r), joinWays(waysToUse).flatten.map { nid => nodes.get(nid) }.flatten)
      z :+ mainWay

    } catch {
      case e: Exception =>
        println("Failed to compose relation: " + r.getId, e.getMessage)
        throw (e)
    }
  }

}
