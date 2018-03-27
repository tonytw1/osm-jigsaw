package resolving

import org.scalatest.FlatSpec

class BoundingBoxSpec extends FlatSpec with BoundingBox with PolygonBuilding {

  val largeArea = makePolygon((-8, 10), (10, -5))

  "bounding box" should "calculate bounding box for polygon" in {
    val box = boundingBoxFor(largeArea)

    assert(box._1 == -8)
    assert(box._2 == 10)
    assert(box._3 == 10)
    assert(box._4 == -5)
  }

}
