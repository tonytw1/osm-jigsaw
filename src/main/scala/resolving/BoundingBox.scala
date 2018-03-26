package resolving

import com.esri.core.geometry.Polygon

trait BoundingBox {

  def boundingBoxFor(p: Polygon): (Double, Double, Double, Double) = {
    var minX = Double.MaxValue  // TODO catch
    var maxY = Double.MinValue
    var maxX = Double.MinValue
    var minY = Double.MaxValue

    val d = p.getCoordinates2D.toSeq
    d.map { p =>
      if (p.x < minX) minX = p.x
      if (p.x > maxX) maxX = p.x

      if (p.y < minY) minY = p.y
      if (p.y > maxY) maxY = p.y
    }

    (minX, maxY, maxX, minY)
  }
}
