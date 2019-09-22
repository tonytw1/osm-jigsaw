package input

import java.io.FileInputStream

import org.apache.logging.log4j.scala.Logging

trait Extracts extends Logging {

  def entireExtractFilepath(extractName: String) = {
    extractName + ".osm.pbf"
  }
  
  def entireExtract(extractName: String): FileInputStream = {
    new FileInputStream(entireExtractFilepath(extractName))
  }

  def nodesFromExtract(extractName: String): FileInputStream = {
    new FileInputStream(nodesExtractFilepath(extractName))
  }

  def waysFromExtract(extractName: String): FileInputStream = {
    new FileInputStream(waysExtractFilepath(extractName))
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
