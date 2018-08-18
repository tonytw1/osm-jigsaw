package resolving

import model.{EntityRendering, JoinedWay}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.mutable

class OutlineBuilder extends EntityRendering with WayJoining with Logging {

  val relationExpander = new RelationExpander()
  val outerWayResolver = new OuterWayResolver()

  // Give a relation resolve it's outer to a seq of consecutively ordered points
  def outlineRings(r: Relation, allRelations: Map[Long, Relation], wayResolver: WayResolver): Seq[Seq[JoinedWay]] = { // TODO handle missing Ways and nodes

    // Attempt to join up the ways (which may be out of order and facing in different directions) into a list consecutive nodes
    def joinWays(ways: Seq[model.Way]): Seq[Seq[JoinedWay]] = {
      val nonEmptyWays: Seq[model.Way] = ways.filter(w => w.nodes.nonEmpty)

      if (nonEmptyWays.nonEmpty) {
        val available = mutable.Set() ++ nonEmptyWays

        def buildRingFromAvailable: Seq[JoinedWay] = {
          val first = available.head
          var joined = Seq(JoinedWay(first, false)) // TODO incorrectly allows non closed areas
          available.remove(first)

          var lastNode = nodesFor(Seq(joined.last)).last

          def nextAttachment(wg: model.Way): Boolean = wg.nodes.head == lastNode || wg.nodes.last == lastNode

          while (available.nonEmpty && available.exists(nextAttachment)) {
            val next = available.find(nextAttachment).get

            if (next.nodes.head == lastNode) {
              joined = joined :+ JoinedWay(next, false)
            } else {
              joined = joined :+ JoinedWay(next, true)
            }

            available.remove(next)
            lastNode = nodesFor(Seq(joined.last)).last
          }

          joined
        }

        var foundRings = Seq[Seq[JoinedWay]]()
        while (available.nonEmpty) {
          val found = buildRingFromAvailable
          val nodes = nodesFor(found)
          val isClosed = nodes.head == nodes.last // TODO duplication
          if (isClosed) {
            foundRings = foundRings :+ found
          } else {
            logger.info("Not closed while outing relation: " + found)
          }
        }
        foundRings

      } else {
        logger.debug("Relation has no non empty outers: " + r.getId)
        Seq()
      }
    }

    val outerWays = outerWayResolver.resolveOuterWayIdsFor(r, allRelations).map { wid =>
      wayResolver.get(wid).map { w =>
        Some(w)
      }.getOrElse {
        logger.warn("Missing way; could not find way " + wid + " required by relation " + r.getId)
        None
      }
    }.flatten

    joinWays(outerWays)
  }

}
