import input.RelationExtractor
import org.apache.commons.cli._
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType}
import output.OsmWriter

import scala.collection.JavaConverters._

object Main {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options  = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilename = cmd.getArgList.get(0) // TODO validate required

    step match {
      case "extract" =>
        extract(inputFilename)

      case _ => {
        println("Unknown step")
        // TODO exit code
      }
    }
  }

  def extract(inputFilename: String) {

    // Predicate to describe the top level relations we wish to extract
    def allAdminBoundaries(entity: Entity): Boolean = {
      val tags = entity.getTags.asScala
      val isAdminLevel = tags.exists(t => t.getKey == "admin_level")
      val isBoundary = tags.exists(t => t.getKey == "type" && t.getValue == "boundary")
      val isBoundaryAdministrativeTag = tags.exists(t => t.getKey == "boundary" && t.getValue == "administrative")
      entity.getType == EntityType.Relation && isAdminLevel && isBoundary && isBoundaryAdministrativeTag // TODO ensure type is tested before more expensive tag operations
    }

    val extractedRelationsWithComponents = new RelationExtractor().extract(inputFilename, allAdminBoundaries)

    val relations = extractedRelationsWithComponents._1
    val ways = extractedRelationsWithComponents._2
    val nodes = extractedRelationsWithComponents._3

    val outputFilepath = "/tmp" + "/" + inputFilename
    new OsmWriter(outputFilepath).write(relations.toSeq ++ ways.toSeq ++ nodes.toSeq)
    println("Dumped found relations and resolved components to: " + outputFilepath)
  }

}
