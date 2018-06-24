package areas

trait BoundingBox {

  def boundingBoxFor(points: Seq[(Double, Double)]): (Double, Double, Double, Double) = {
    var latitudes = points.map(_._1)
    var longitudes = points.map(_._2)

    val minX = latitudes.min
    val maxX = latitudes.max
    val minY = longitudes.min
    val maxY = longitudes.max

    (minX, maxY, maxX, minY)
  }

}
