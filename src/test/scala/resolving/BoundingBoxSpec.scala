package resolving

import com.esri.core.geometry._
import org.scalatest.FlatSpec

class BoundingBoxSpec extends FlatSpec with BoundingBox {

  val largeArea = new Polygon()
  largeArea.startPath(-8, -5)
  largeArea.lineTo(10, -5)
  largeArea.lineTo(10, 10)
  largeArea.lineTo(-8, 10)

  "bounding box" should "calculate bounding box for polygon" in {
    val box = boundingBoxFor(largeArea)

    assert(box._1 == -8)
    assert(box._2 == 10)
    assert(box._3 == 10)
    assert(box._4 == -5)
  }

}
