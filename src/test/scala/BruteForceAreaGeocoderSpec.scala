import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import input.TestValues
import model.{Area, EntityRendering}
import org.scalatest.FlatSpec

class BruteForceAreaGeocoderSpec extends FlatSpec with TestValues with EntityRendering {

  "geocode" should "build readable place names for point locations" in {

    var areasOutputFile = "/tmp/areas.ser"

    val ois = new ObjectInputStream(new FileInputStream(areasOutputFile))
    val areas = ois.readObject.asInstanceOf[Set[Area]]
    ois.close

    println("Checking locations against " + areas.size + " areas")
    val sr = SpatialReference.create(1)

    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds, newYork, halfDome).map { location =>
      val pt = new Point(location._1, location._2)

      val areasContainingLocation = areas.filter { a =>
        OperatorContains.local().execute(a.polygon, pt, sr, null)
      }.toSeq


      val sorted = areasContainingLocation.sortWith{(a, b) =>
        OperatorContains.local().execute(b.polygon, a.polygon, sr, null)
      }

      println(location + ": " + sorted.map(a => a.name).mkString(", "))
    }

    succeed
  }

}
