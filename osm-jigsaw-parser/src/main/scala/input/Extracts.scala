package input

import java.io.FileInputStream

trait Extracts {

  def entireExtract(inputFilepath: String) = {
    new FileInputStream(inputFilepath)
  }

  def nodesFromExtract(inputFilePath: String): FileInputStream = {
    new FileInputStream(nodesExtractFilepath(inputFilePath))
  }

  def waysFromExtract(inputFilePath: String): FileInputStream = {
    new FileInputStream(waysExtractFilepath(inputFilePath))
  }

  def relationExtractFilepath(inputFilepath: String): String = {
    inputFilepath + ".relations"
  }

  def waysExtractFilepath(inputFilepath: String): String = {
    inputFilepath + ".ways"
  }

  def nodesExtractFilepath(inputFilepath: String): String = {
    inputFilepath + ".nodes"
  }

}
