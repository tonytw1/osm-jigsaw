package graph

import java.io.BufferedInputStream
import java.net.URL

import model.{Area, OsmIdParsing, Point}
import outputarea.OutputArea
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class AreasReader extends OsmIdParsing {

  def loadAreas(areasFile: URL): Map[Long, Area] = {

    def outputAreaToArea(oa: OutputArea): Option[Area] = {
      oa.id.flatMap { id =>
        oa.area.map { a =>
          val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
          Area(id = id, points = points, osmIds = oa.osmIds.map(toOsmId), area = a)
        }
      }
    }

    val areasMap = mutable.Map[Long, Area]()
    val planet = Area(id = 0L, points = Seq.empty, osmIds = Seq.empty, area = 0.0)  // TODO meh
    areasMap += 0L -> planet
    val input = new BufferedInputStream(areasFile.openStream())
    val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
    var ok = true
    while (ok) {
      counter.withProgress {
        val added = OutputArea.parseDelimitedFrom(input).flatMap { oa =>
          outputAreaToArea(oa).map { area =>
            areasMap += area.id -> area
            area
          }
        }
        ok = added.nonEmpty
      }
    }
    Logger.info("Mapped areas: " + areasMap.size)
    input.close()
    areasMap.toMap
  }

}
