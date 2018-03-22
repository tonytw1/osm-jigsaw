package input

import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.JavaConverters._
import scala.collection.mutable

class OuterNodeMapper(ways: Map[Long, Way], nodes: Map[Long, Node]) {

  def outerNodesFor(r: Relation): Seq[Node] = { // TODO handle missing Ways and nodes

    try {
      def resolveWaysFor(wayRelationMembers: Seq[RelationMember]): Seq[Option[Way]] = {
        wayRelationMembers.map { rm =>
          ways.get(rm.getMemberId)
        }
      }

      val relationMembers = r.getMembers.asScala
      val nonNodes = relationMembers.filter(rm => rm.getMemberType != EntityType.Node)
      val outers = nonNodes.filter(rm => rm.getMemberRole == "outer")

      outers.filter(rm => rm.getMemberType == EntityType.Relation).map { r =>
        println("Need to resolve sub relation in outer role: " + r)
      }

      val outerWayRelationMembers = nonNodes.filter(rm => rm.getMemberType == EntityType.Way) // TODO resolve sub relations

      val outerWays: Seq[Option[Way]] = resolveWaysFor(outerWayRelationMembers)

      val wayGroups: Seq[mutable.Seq[Long]] = outerWays.flatten.map { w =>

        val nodes: mutable.Seq[Long] = w.getWayNodes.asScala.map(wn => wn.getNodeId)
        nodes
      }.filter(wg => wg.nonEmpty)

      val joinedNodeIds = if (wayGroups.size < 2) {
        val x = wayGroups.map(i => i).flatten
        x

      } else {

        val headGroup = wayGroups(0)
        val secondGroup = wayGroups(1)

        val first = headGroup.head
        val last = headGroup.last



        var outerNodeIds = if (!(secondGroup.contains(last))) {
          wayGroups(0).reverse
        } else {
          wayGroups(0)
        }



        wayGroups.drop(1).map { wg =>
          val z = outerNodeIds.lastOption.map { l =>
            //println(l + " v " + wg.head + " / " + wg.last)

            if (l != wg.head && l != wg.last) {
              throw new RuntimeException("Can join ways; no common nodes when joining: " + wg)
            }

            if (wg.head == l) {
              outerNodeIds = outerNodeIds ++ wg

            } else {
              //println("!!!!!!!!! Nose to tail way join detected")
              outerNodeIds = outerNodeIds ++ wg.reverse

            }

          }
          z

        }
        outerNodeIds
      }

      joinedNodeIds.map { nid =>
        nodes.get(nid)
      }.flatten

    } catch {
      case e: Exception =>
        println("Failed to compose relation: " + r.getId, e.getMessage)
        Seq()
    }
  }

}
