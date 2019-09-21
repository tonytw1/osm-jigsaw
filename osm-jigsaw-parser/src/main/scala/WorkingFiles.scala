import java.io.{FileOutputStream, OutputStream}

trait WorkingFiles {

  def namedNodesFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath: String)
  }

  def tagsFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath)
  }

}
