import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import input.TestValues
import model.Area
import org.scalatest.FlatSpec

class GeocoderSpec extends FlatSpec with TestValues with EntityRendering {

  "geocode" should "build readable place names for point locations" in {

    var areasOutputFile = "/tmp/areas.ser"

    val ois = new ObjectInputStream(new FileInputStream(areasOutputFile))
    val areas = ois.readObject.asInstanceOf[Seq[Area]]
    ois.close

    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds, newYork, halfDome).map { location =>
      val pt = new Point(location._1, location._2)

      val areasContainingLocation: Seq[Area] = areas.filter { a =>
        val area = a.polygon
        val sr = SpatialReference.create(1)
        OperatorContains.local().execute(area, pt, sr, null)
      }

      println(location + ": " + areasContainingLocation.map(a => a.name).mkString(", "))
    }

    succeed
  }

}
