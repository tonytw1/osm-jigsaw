package input

import java.io.{File, FileInputStream, InputStream}

import com.google.common.io.Files
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType

trait Extracts extends Logging with Boundaries {

  def entireExtractFilepath(extractName: String) = {
    extractName + ".osm.pbf"
  }
  
  def entireExtract(extractName: String): FileInputStream = {
    new FileInputStream(entireExtractFilepath(extractName))
  }

  def nodesFromExtract(extractName: String): InputStream = {
    val startOfNodes = readBoundaries.get(EntityType.Node.name()).get
    val startOfWays = readBoundaries.get(EntityType.Way.name()).get
    Files.asByteSource(new File(entireExtractFilepath(extractName))).slice(startOfNodes, startOfWays - startOfNodes).openStream()
  }

  def waysFromExtract(extractName: String): InputStream = {
    val startOfWays = readBoundaries.get(EntityType.Way.name()).get
    val startOfRelations = readBoundaries.get(EntityType.Relation.name()).get
    Files.asByteSource(new File(entireExtractFilepath(extractName))).slice(startOfWays, startOfRelations - startOfWays).openStream()
  }

  def relationsFromExtract(extractName: String): InputStream = {
    val startOfRelations = readBoundaries.get(EntityType.Relation.name()).get
    val eof = readBoundaries.get("EOF").get
    Files.asByteSource(new File(entireExtractFilepath(extractName))).slice(startOfRelations, eof - startOfRelations).openStream()
  }

  def relationExtractFilepath(extractName: String): String = {
    entireExtractFilepath(extractName) + ".relations"
  }

  def waysExtractFilepath(extractName: String): String = {
    entireExtractFilepath(extractName) + ".ways"
  }

  def nodesExtractFilepath(extractName: String): String = {
    entireExtractFilepath(extractName) + ".nodes"
  }

}
