package resolving

import model.EntityRendering
import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.mutable

class OutlineBuilder extends EntityRendering {

  val relationExpander = new RelationExpander()
  val outerWayResolver = new OuterWayResolver()

  // Give a relation resolve it's outer to a seq of consecutively ordered points
  def outlineNodesFor(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Seq[(String, String, Seq[(Double, Double)])] = { // TODO handle missing Ways and nodes

    // Attempt to join up the ways (which may be out of order and facing in different directions) into a list consecutive nodes
    def joinWays(ways: Seq[model.Way]): Seq[Seq[Seq[Long]]] = {
      val nonEmptyWays = ways.filter(w => w.nodes.nonEmpty)

      if (nonEmptyWays.nonEmpty) {
        val available = mutable.Set() ++ nonEmptyWays

        def buildRingFromAvailable: Seq[Seq[Long]] = {
          val first = available.head
          var joined = Seq(first.nodes)   // TODO incorrectly allows non closed areas
          available.remove(first)

          def nextAttachment(wg: model.Way): Boolean = wg.nodes.head == joined.last.last || wg.nodes.last == joined.last.last

          while (available.nonEmpty && available.exists(nextAttachment)) {
            val next = available.find(nextAttachment).get
            if (next.nodes.head == joined.last.last) {
              joined = joined :+ next.nodes
            } else {
              joined = joined :+ next.nodes.reverse
            }
            available.remove(next)
          }

          joined
        }

        var foundRings = Seq[Seq[Seq[Long]]]()
        while (available.nonEmpty) {
          val found: Seq[Seq[Long]] = buildRingFromAvailable

          val isClosed = found.flatten.head == found.flatten.last
          if (isClosed) {
            foundRings = foundRings :+ found
          } else {
            println("Not closed while outinging relation " + r + ": " + found)
          }
        }

        foundRings


      } else {
        println("Relation has no non empty outers: " + r.getId)
        Seq()
      }
    }

    try {
      val outerWays: Seq[model.Way] = outerWayResolver.resolveOuterWayIdsFor(r, allRelations).map { wid =>
        ways.get(wid)
      }.flatten // TODO handle missing ways

      /*
      val closedWays = outerWays.filter { w =>
        w._3.head == w._3.last
      }

      val waysToUse: Seq[(String, String, Seq[Long])] = if (outerWays.size > 1) {
        val excludingClosedWays = outerWays.filterNot(w =>
          closedWays.contains(w)
        )
        excludingClosedWays

      } else {
        outerWays
      }

      val z = closedWays.map { cw =>
        cw._3.map(nid => nodes.get(nid)).flatten
        (cw._1, cw._2, cw._3.map(nid => nodes.get(nid)).flatten)
      }
      */

      val rings = joinWays(outerWays).map { x =>
        (render(r), r.getId() + "Relation", x.flatten.map { nid => nodes.get(nid) }.flatten)
      }

      rings

    } catch {
      case e: Exception =>
        println("Failed to compose relation: " + r.getId, e.getMessage)
        throw (e)
    }
  }

}
