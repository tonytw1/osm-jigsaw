package graph

import java.io.{BufferedInputStream, File, FileInputStream}

import outputarea.OutputArea

import scala.collection.mutable

class GraphReader {

  def loadGraph(file: File): Area = {
    val is = new BufferedInputStream(new FileInputStream(file))

    val head = Area(None, None)
    val stack = mutable.Stack[Area]()
    stack.push(head)

    var ok = true
    while (ok) {
      val outputArea = OutputArea.parseDelimitedFrom(is)
      outputArea.map { a =>
        val area = Area(id = a.id, name = a.name)

        var insertInto = stack.pop
        while (insertInto.id != a.parent) {
          insertInto = stack.pop
        }

        insertInto.children += area
        stack.push(insertInto)
        stack.push(area)

        println(stack.map(a => a.name).flatten.reverse.mkString(" / "))
      }
      ok = outputArea.nonEmpty
    }

    head
  }

}

case class Area(id: Option[String] = None, name: Option[String] = None, children: mutable.Set[Area] = mutable.Set())
