package output

import model.Area
import outputarea.OutputArea

import java.io.OutputStream
import scala.collection.mutable

trait AreaWriting {

  def exportArea(area: Area, output: OutputStream): Unit = {
    val latitudes = mutable.ListBuffer[Double]()
    val longitudes = mutable.ListBuffer[Double]()
    val pointCount = area.polygon.getPointCount - 1
    (0 to pointCount).map { i =>
      val p = area.polygon.getPoint(i)
      latitudes.+=(p.getX)
      longitudes.+=(p.getY)
    }

    OutputArea(id = Some(area.id), osmIds = area.osmIds, latitudes = latitudes, longitudes = longitudes, area = Some(area.area)).writeDelimitedTo(output)
  }

}
