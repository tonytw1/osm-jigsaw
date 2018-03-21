package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._

class OsmSinkSpec extends FlatSpec {

  val EUROPE = "europe-latest.osm.pbf"
  val GREAT_BRITAIN = "great-britain-latest.osm.pbf"
  val IRELAND = "ireland-and-northern-ireland-latest.osm.pbf"
  val deferencedOutputFile = "/tmp/out.pbf"

  def all(entity: Entity): Boolean = true


  "osm parser" should "extract high level relations from OSM file and deferences those resolve the nodes needed to outline those relations" in {
    val inputFilePath = GREAT_BRITAIN
    val reader = new OsmReader(inputFilePath)

    val found = reader.read(allAdminBoundaries)
    found.map { entity =>
      println("Found: " + entity.getId + entity.getType + " " + entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue))
    }

    var deferenced = new RelationDeferencer().dereference(found.toSet, inputFilePath)

    // In theory found now contains a list of interesting relations and deferenced contains all of the nodes required to draw the shapes of those relations
    // If we can dump these to disk that would allow faster iteration of the next step
    new OsmWriter(deferencedOutputFile).write(deferenced.toSeq)

    succeed
  }

  "osm parser" should "build bounding boxes for relations" in {
    val entities = new OsmReader(deferencedOutputFile).read(all)

    val relations = entities.map { e =>
      e match {
        case r: Relation => Some(r)
        case _ => None
      }
    }.flatten

    val ways = entities.map { e =>
      e match {
        case w: Way => Some(w)
        case _ => None
      }
    }.flatten.map { i =>
      (i.getId, i)
    }.toMap

    val nodes = entities.map { e =>
      e match {
        case n: Node => Some(n)
        case _ => None
      }
    }.flatten.map { i =>
      (i.getId, i)
    }.toMap

    println("Found " + relations.size + " relations to process")

    Range(1, 11).map { i =>
      println(i + " --------------------------------------------------------")

      val adminLevelRelations = relations.filter { r =>
        r.getTags.asScala.exists(t => t.getKey == "admin_level" && t.getValue == i.toString)
      }

      adminLevelRelations.map { r =>
        val outerNodes = new OuterNodeMapper(ways, nodes).outerNodesFor(r)
        val latitudes = outerNodes.map(n => n.getLatitude)
        val longitudes = outerNodes.map(n => n.getLongitude)
        val boundingBox = ((latitudes.max, longitudes.max), (latitudes.min, longitudes.min))

        println(r.getTags.asScala.find(t => t.getKey == "name").map(t => t.getValue) + ": " + boundingBox)
      }
    }

    succeed
  }

  def allRels(entity: Entity): Boolean = {
    topLevelRelations(entity) || secondLevelRelations(entity) || thirdLevelRelations(entity) || levelEight(entity)
  }

  def allAdminBoundaries(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevel= tags.exists(t => t.getKey == "admin_level")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
    entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag
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

  def levelEight(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevelFour = tags.exists(t => t.getKey == "admin_level" && t.getValue == "8")
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

  def missingNode(entity: Entity): Boolean = {
    entity.getId == 2081060315 && entity.getType == EntityType.Node
  }

  def suburb(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isSuburb = tags.exists(t => t.getKey == "place" && t.getValue == "suburb")
    isSuburb
  }

}
