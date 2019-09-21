import java.io.{FileOutputStream, OutputStream}

trait WorkingFiles {

  def namedNodesFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath: String)
  }

  def tagsFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath)
  }

  def extractedRelsFilepath(extractName: String) = {
    extractName + ".rels.pdf"
  }

  def areaWaysFilepath(extractName: String) = {
    extractName + ".areaways.pdf"
  }

  def areaWaysWaysFilePath(extractName: String) = {
    areaWaysFilepath(extractName) + ".ways.pbf"
  }

}
