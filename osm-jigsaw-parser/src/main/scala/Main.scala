import java.io._

import graphing.EntitiesToGraph
import input._
import model.EntityRendering
import org.apache.commons.cli._
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6._
import outputnode.OutputNode
import outputresolvedarea.OutputResolvedArea
import play.api.libs.json.Json
import progress.CommaFormattedNumbers
import resolving._
import steps._


object Main extends EntityRendering with Logging with PolygonBuilding
  with ProtocolbufferReading with WayJoining with CommaFormattedNumbers with EntityOsmId
  with Extracts with WorkingFiles with EntitiesToGraph with AreaReading {

  private val STEP = "s"

  val parser = new DefaultParser()
  val options = new Options()
  options.addOption(STEP, true, "Which step to apply to the input file")

  def main(args: Array[String]): Unit = {
    val cmd = parser.parse(options, args)
    val step = cmd.getOptionValue(STEP)

    val inputFilepath = cmd.getArgList.get(0) // TODO validation required

    step match {
      case "stats" => stats(inputFilepath)
      case "boundaries" => new FindBoundaries().findEntityBoundaries(inputFilepath)
      case "extract" => extract(inputFilepath)
      case "namednodes" => extractNamedNodes(inputFilepath, cmd.getArgList.get(1))
      case "areaways" => new ExtractAreas().resolveAreaWays(inputFilepath)
      case "areastats" => areaStats(inputFilepath)
      case "areas" => new RenderAndDeduplicateAreas().resolveAreas(inputFilepath)
      case "graph" => new BuildGraph().buildGraph(inputFilepath)
      case "tags" => new ExtractAreaTags().tags(inputFilepath)
      case "rels" => {
        val relationIds = cmd.getArgList.get(2).split(",").map(s => s.toLong).toSeq
        extractRelations(inputFilepath, cmd.getArgList.get(1), relationIds)
      }
      case _ => logger.info("Unknown step") // TODO exit code
    }
  }

  def stats(inputFilepath: String) = {
    var nodes = 0L
    var ways = 0L
    var relations = 0L

    var namedNodes = 0L
    var namedWays = 0L
    var namedRelations = 0L

    def countTypes(entity: Entity) = {
      entity match {
        case n: Node => nodes = nodes + 1
        case w: Way => ways = ways + 1
        case r: Relation => relations = relations + 1
        case _ =>
      }

      if (hasName(entity)) {
        entity match {
          case n: Node => namedNodes = namedNodes + 1
          case w: Way => namedWays = namedWays + 1
          case r: Relation => namedRelations = namedRelations + 1
          case _ =>
        }
      }
    }

    def all(entity: Entity): Boolean = true

    new SinkRunner(entireExtract(inputFilepath), all, countTypes).run

    logger.info("Nodes: " + namedNodes + " / " + nodes)
    logger.info("Ways: " + namedWays + " / " + ways)
    logger.info("Relations: " + namedRelations + " / " + relations)
  }

  def areaStats(inputFilepath: String) = {

    def print(ra: OutputResolvedArea) = {
      ra.ways.foreach { l =>
        println(ra.id.get + "," + l)
      }
    }

    def read(inputStream: InputStream) = OutputResolvedArea.parseDelimitedFrom(inputStream)

    processPbfFile(inputFilepath, read, print)
  }

  def extractNamedNodes(inputFilepath: String, outputFilepath: String): Unit = {
    logger.info("Extracting named nodes")

    val namedNodesOutput = new BufferedOutputStream(namedNodesFile(outputFilepath))

    def writeToNamedNodesFile(entity: Entity) = {
      entity match {
        case n: Node => OutputNode(osmId = Some(osmIdFor(n)), latitude = Some(n.getLatitude), longitude = Some(n.getLongitude)).writeDelimitedTo(namedNodesOutput)
        case _ =>
      }
    }

    def named(entity: Entity): Boolean = {
      hasName(entity)
    }

    new SinkRunner(nodesFromExtract(inputFilepath), named, writeToNamedNodesFile).run

    namedNodesOutput.flush()
    namedNodesOutput.close()
    logger.info("Done")
  }

  def extract(extractName: String) {

    def recordRecursiveRelations(extractName: String, relationIds: Seq[Long]): Unit = {
      val recursiveRelationsFile = new FileOutputStream(recursiveRelationsFilepath(extractName))
      recursiveRelationsFile.write(Json.toBytes(Json.toJson(relationIds)))
      recursiveRelationsFile.close()
    }

    val outputFilepath = extractedRelsFilepath(extractName)
    logger.info("Extracting entities and their resolved components from " + extractName + " into " + outputFilepath)

    val extractor = new RelationExtractor()
    extractor.extract(extractName, entitiesToGraph, outputFilepath)

    logger.info("Dumping discovered recursive relations")
    recordRecursiveRelations(extractName, extractor.recursiveRelations())

    logger.info("Done")
  }

  def extractRelations(extractName: String, outputFilepath: String, relationIds: Seq[Long]): Unit = {
    logger.info("Extracting specific relations: " + relationIds)

    def selectedRelations(entity: Entity): Boolean = {
      entity.getType == EntityType.Relation && relationIds.contains(entity.getId)
    }

    new RelationExtractor().extract(extractName, selectedRelations, outputFilepath)

    logger.info("Done")
  }

}
