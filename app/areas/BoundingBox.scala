package areas

import graph.Point

trait BoundingBox {

  def boundingBoxFor(points: Seq[Point]): (Double, Double, Double, Double) = {
    var latitudes = points.map(_.lat)
    var longitudes = points.map(_.lon)

    val minX = latitudes.min
    val maxX = latitudes.max
    val minY = longitudes.min
    val maxY = longitudes.max

    (minX, maxY, maxX, minY)
  }

}
