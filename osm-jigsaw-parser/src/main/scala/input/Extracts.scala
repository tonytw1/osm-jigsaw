package input

trait Extracts {

  def relationExtract(inputFilepath: String) = {
    inputFilepath + ".relations"
  }

  def waysExtract(inputFilepath: String) = {
    inputFilepath + ".ways"
  }

  def nodesExtract(inputFilepath: String) = {
    inputFilepath + ".nodes"
  }

}
