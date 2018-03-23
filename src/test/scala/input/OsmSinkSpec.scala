package input

import com.esri.core.geometry._
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.collection.mutable

class OsmSinkSpec extends FlatSpec with TestValues {

  val EUROPE = "europe-latest.osm.pbf"
  val GREAT_BRITAIN = "great-britain-latest.osm.pbf"
  val IRELAND = "ireland-and-northern-ireland-latest.osm.pbf"
  val deferencedOutputFile = "/tmp/out.pbf"

  def all(entity: Entity): Boolean = true

  "osm parse" should "can fit all relations, Way members and Node postions in RAM" in {
    val inputFilePath = GREAT_BRITAIN
    val sink = new OsmRelationCachingSink(all)
    val reader = new OsmReader(inputFilePath, sink)
    reader.read
    val allRelations = sink.relations
    println("Cached " + allRelations.size + " relations")

    val foundRelations: Set[Relation] = allRelations.values.filter(allAdminBoundaries).toSet
    println("Found " + foundRelations.size + " admin boundaries")

    val wayIds = foundRelations.map { r =>
      new RelationResolver().resolveOuterWays(r)
    }.flatten

    println("Need " + wayIds.size + " ways to bound relations")
    def requiredWays(entity: Entity): Boolean = entity.getType == EntityType.Way && wayIds.contains(entity.getId)

    val sink3 = new OsmEntitySink(requiredWays)
    val reader3 = new OsmReader(inputFilePath, sink3)
    reader3.read
    val ways = sink3.found
    println("Found " + ways.size + " ways")

    val nodeIds: mutable.Set[Long] = ways.map { e =>
      e match {
        case w: Way =>
          w.getWayNodes.asScala.map(wn => wn.getNodeId).toSet
        case _ => Set()
      }
    }.flatten

    println("Need " + nodeIds.size + " nodes to bound relations")
    def requiredNodes(entity: Entity): Boolean = entity.getType == EntityType.Node && nodeIds.contains(entity.getId)

    val sink4 = new OsmEntitySink(requiredNodes)
    val reader4 = new OsmReader(inputFilePath, sink4)
    reader4.read
    val nodes = sink4.found
    println("Found " + nodes.size + " nodes")

    new OsmWriter(deferencedOutputFile).write(foundRelations.toSeq ++ ways.toSeq ++ nodes.toSeq)
    println("Dumped found relations and resolved components to: " + deferencedOutputFile)
    succeed
  }

  /*
  "osm parser" should "extract high level relations from OSM file and deferences those resolve the nodes needed to outline those relations" in {
    val inputFilePath = GREAT_BRITAIN
    val sink = new OsmEntitySink(allAdminBoundaries)
    val reader = new OsmReader(inputFilePath, sink)

    reader.read
    val found = sink.found
    found.map { entity =>
      println("Found: " + entity.getId + entity.getType + " " + entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue))
    }

    var deferenced = new RelationDeferencer().dereference(found.toSet, inputFilePath)

    // In theory found now contains a list of interesting relations and deferenced contains all of the nodes required to draw the shapes of those relations
    // If we can dump these to disk that would allow faster iteration of the next step
    new OsmWriter(deferencedOutputFile).write(deferenced.toSeq)

    succeed
  }
  */

  /*
  "osm parser" should "meh" in {

    val pt = new Point(0.5, 0.5)

    val area = new Polygon()
    area.startPath(0, 0)
    area.lineTo(0, 1)
    area.lineTo(1, 1)
    area.lineTo(1, 0)
    println(area)

    val sr: SpatialReference = SpatialReference.create(1)
    val contains: Boolean = OperatorContains.local().execute(area, pt, sr, null)
    println(contains)
  }
  */

  /*
  "osm parser" should "build bounding boxes for relations" in {
    val sink = new OsmEntitySink(all)
    new OsmReader(deferencedOutputFile, sink).read
    val entities = sink.found

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

    val boundedRelations = relations.map { r =>
      val outerNodes = new OuterNodeMapper(ways, nodes).outerNodesFor(r)
      outerNodes.headOption.map { h =>
        val area = new Polygon()
        area.startPath(h.getLatitude, h.getLongitude)
        outerNodes.drop(1).map { on =>
          val pt = new Point(on.getLatitude, on.getLongitude)
          area.lineTo(pt)
        }
        (r, area)
      }

    }.flatten

    val london = (51.506, -0.106)
    val twickenham = (51.4282594, -0.3244692)
    val bournmouth = (50.720, -1.879)
    val lyndhurst = (50.8703, -1.5942)
    val edinburgh = (55.9518, -3.1840)
    val newport = (50.6995, -1.2935)
    val pembroke = (51.6756, -4.9122)
    val leeds = (53.7868, -1.5528)

    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds).map { location =>
      val components = Range(1, 11).map { i =>
        val bound = boundedRelations.find { b =>
          val r = b._1
          val adminLevel = r.getTags.asScala.find(t => t.getKey == "admin_level").map(t => t.getValue)

          val area = b._2
          val pt = new Point(location._1, location._2)


          val sr: SpatialReference = SpatialReference.create(1)
          val contains: Boolean = OperatorContains.local().execute(area, pt, sr, null)

          adminLevel == Some(i.toString) && contains
        }
        bound
      }.flatten.map(_._1).reverse

      println(location + ": " + components.map(r => render(r)).mkString(", "))
    }

    succeed
  }
  */


  def render(entity: Entity): String = {
    entity.getTags.asScala.find(t =>t.getKey == "name").map(t => t.getValue).getOrElse(entity.getId + entity.getType.toString)
  }

  def allRels(entity: Entity): Boolean = {
    topLevelRelations(entity) || secondLevelRelations(entity) || thirdLevelRelations(entity) || levelEight(entity)
  }

  def outOfOrderRelationId = 7181767

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

  def richmond(entity: Entity): Boolean = {
    entity.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES._1 && entity.getType == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES._2
  }

  def suburb(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isSuburb = tags.exists(t => t.getKey == "place" && t.getValue == "suburb")
    isSuburb
  }


}
