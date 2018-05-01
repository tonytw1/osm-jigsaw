package graph

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

import outputarea.OutputArea

import scala.collection.mutable

class GraphReader {

  def loadGraph(input: InputStream): Area = {
    val head = Area(None, None)
    val stack = mutable.Stack[Area]()
    stack.push(head)

    var ok = true
    while (ok) {
      val outputArea = OutputArea.parseDelimitedFrom(input)
      outputArea.map { a =>
        val points: Seq[Point] = (a.latitudes zip a.longitudes).map(ll => Point(ll._1, ll._2))

        val area = Area(id = a.id, name = a.name, points = points)

        var insertInto = stack.pop
        while (insertInto.id != a.parent) {
          insertInto = stack.pop
        }

        insertInto.children += area
        stack.push(insertInto)
        stack.push(area)

        //println(stack.map(a => a.name).flatten.reverse.mkString(" / "))
      }
      ok = outputArea.nonEmpty
    }

    input.close()
    head
  }

}

case class Area(id: Option[String] = None, name: Option[String] = None, children: mutable.Set[Area] = mutable.Set(), points: Seq[Point]= Seq())
case class Point(latitude: Double, longitude: Double)