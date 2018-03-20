package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._

class OsmSinkSpec extends FlatSpec {

  val EUROPE_LATEST = "europe-latest.osm.pbf"
  val GREAT_BRITAIN = "great-britain-latest.osm.pbf"
  val IRELAND = "ireland-and-northern-ireland-latest.osm.pbf"

  val reader = new OsmReader(GREAT_BRITAIN)

  "osm sink" should "read pdf file" in {
    val found = reader.read(thirdLevelRelations)
    found.map { f =>
      println("Found: " + f.getId + f.getType + " " + f.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue))
    }
    var cleaned = found
    while(cleaned.exists(e => e.getType != EntityType.Node)) {
      cleaned = dereference(cleaned)
    }

    // in theory cleaned now contains all of the nodes required to draw the shapes of the top level relations
    succeed
  }

  case class OsmId(entityType: EntityType, id: Long)

  def dereference(entites: Seq[Entity]): Seq[Entity] = {
    val flattenings: Set[OsmId] = entites.flatMap { e =>
      e match {
        case r: Relation =>
          r.getMembers.asScala.map { rm =>
            OsmId(rm.getMemberType, rm.getMemberId)
          }
        case w: Way =>
          w.getWayNodes.asScala.map { wn =>
            OsmId(EntityType.Node, wn.getNodeId)
          }
        case n: Node =>
          Seq(OsmId(EntityType.Node, n.getId))
        case _ =>
          Seq()
      }
    }.toSet

    println("Entities flatten to: " + flattenings.size)

    def theseEntites(entity: Entity): Boolean = {
      flattenings.contains(OsmId(entity.getType, entity.getId))
    }

    val found = reader.read(theseEntites)
    println("Found " + found.size + " flattened entities")
    found
  }

  def topLevelRelations(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevelTwo = tags.exists(t => t.getKey == "admin_level" && t.getValue == "2")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
    entity.getType == EntityType.Relation && isAdminLevelTwo && isBoundary && isBoundaryAdministrativeTag
  }

  def secondLevelRelations(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevelFour = tags.exists(t => t.getKey == "admin_level" && t.getValue == "4")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
    entity.getType == EntityType.Relation && isAdminLevelFour && isBoundary && isBoundaryAdministrativeTag
  }

  def thirdLevelRelations(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevelFour = tags.exists(t => t.getKey == "admin_level" && t.getValue == "6")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
    entity.getType == EntityType.Relation && isAdminLevelFour && isBoundary && isBoundaryAdministrativeTag
  }

  def isleOfMan(entity: Entity): Boolean = {
    entity.getId == 62269 && entity.getType == EntityType.Relation
  }

}
