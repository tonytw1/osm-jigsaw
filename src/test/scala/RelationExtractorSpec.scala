package input

import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import output.OsmWriter

import scala.collection.JavaConverters._

class RelationExtractorSpec extends FlatSpec with TestValues {

  val EUROPE = "europe-latest.osm.pbf"
  val GREAT_BRITAIN = "great-britain-latest.osm.pbf"
  val IRELAND = "ireland-and-northern-ireland-latest.osm.pbf"
  val NORTH_AMERICA = "north-america-latest.osm.pbf"

  val outputFolder = "/tmp"

  "relation extractor" should "extract requested relations and their component ways and nodes from osm pbf extract file" in {
    val inputFilename = GREAT_BRITAIN

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilename, allAdminBoundaries)

    val relations = extractedRelationsWithComponents._1
    val ways = extractedRelationsWithComponents._2
    val nodes = extractedRelationsWithComponents._3

    val outputFilepath = outputFolder + "/" + inputFilename
    new OsmWriter(outputFilepath).write(relations.toSeq ++ ways.toSeq ++ nodes.toSeq)
    println("Dumped found relations and resolved components to: " + outputFilepath)
    succeed
  }

  // Predicate to describe the top level relations we wish to extract
  def allAdminBoundaries(entity: Entity): Boolean = {
    val tags = entity.getTags.asScala
    val isAdminLevel = tags.exists(t => t.getKey == "admin_level")
    val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
    val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
    entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag
  }

}
