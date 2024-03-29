package graph

import model.{Area, OsmIdParsing, Point}
import outputarea.OutputArea
import play.api.{Configuration, Logger}
import progress.ProgressCounter

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject
import scala.collection.mutable

class AreasReader @Inject()(configuration: Configuration) extends OsmIdParsing {

  def loadAreas(areasFile: URL): Map[Long, Area] = {
    Logger.info("Loading areas from: " + areasFile.toExternalForm)

    def outputAreaToArea(oa: OutputArea): Option[Area] = {
      oa.id.flatMap { id =>
        oa.area.map { a =>
          val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
          Area(id = id, points = points, osmIds = oa.osmIds.map(toOsmId), area = a)
        }
      }
    }

    val areasMap = mutable.Map[Long, Area]()
    val planet = Area(id = -1L, points = Seq.empty, osmIds = Seq.empty, area = 0.0) // TODO If we need this root node then it should be in the graph and areas file
    areasMap += planet.id -> planet
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
