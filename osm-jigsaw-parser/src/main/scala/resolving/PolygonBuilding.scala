package resolving

import com.esri.core.geometry.{Point, Polygon}

trait PolygonBuilding {

  def makePolygon(topLeft: (Int, Int), bottomRight: (Int, Int)): Polygon = {
    val polygon = new Polygon()
    polygon.startPath(topLeft._1, topLeft._2)
    polygon.lineTo(topLeft._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, topLeft._2)
    polygon
  }


  def makePolygonD(topLeft: (Double, Double), bottomRight: (Double, Double)): Polygon = {
    val polygon = new Polygon()
    polygon.startPath(topLeft._1, topLeft._2)
    polygon.lineTo(topLeft._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, topLeft._2)
    polygon
  }

  def polygonForPoints(outerPoints: Seq[(Double, Double)]): Option[Polygon] = {
    val polygon = outerPoints.headOption.map { n =>
      val area = new Polygon()
      area.startPath(n._1, n._2)
      outerPoints.drop(1).map { on =>
        area.lineTo(new Point(on._1, on._2))
      }
      area
    }

    /*
    val c = polygon.map { p =>
      import com.esri.core.geometry.OperatorSimplifyOGC
      val simplePolygon: Polygon = OperatorSimplifyOGC.local.execute(p, null, true, null).asInstanceOf[Polygon]
      simplePolygon
    }
    */

    polygon
  }

}
