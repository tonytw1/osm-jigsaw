package input

import java.io.{FileOutputStream, OutputStream}

trait WorkingFiles {

  def boundariesFilepath(extractName: String): String = {
    extractName + ".boundaries.json"
  }

  def recursiveRelationsFilepath(extractName: String): String = {
    extractName + ".recursive-relations.json"
  }

  def namedNodesFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath: String)
  }

  def tagsFile(outputFilepath: String): OutputStream = {
    new FileOutputStream(outputFilepath)
  }

  def extractedRelsFilepath(extractName: String) = {
    extractName + ".rels.pbf"
  }

  def areaWaysFilepath(extractName: String) = {
    extractName + ".areaways.pbf"
  }

  def areaWaysWaysFilePath(extractName: String) = {
    areaWaysFilepath(extractName) + ".ways.pbf"
  }

  def areasFilePath(extractName: String): String = {
    extractName + ".areas.pbf"
  }

  def tagsFilePath(extractName: String): String = {
    extractName + ".tags.pbf"
  }

}
