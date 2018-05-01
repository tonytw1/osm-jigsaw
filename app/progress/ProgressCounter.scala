package progress

import java.text.DecimalFormat

import org.joda.time.DateTime
import play.api.Logger

class ProgressCounter(step: Int, total: Option[Long] = None, label: Option[String] = None) {
  val nf = new DecimalFormat()

  var i = 0L
  var j = 0
  var low = DateTime.now()

  def withProgress[R](f: => R): R = {
    i = i + 1
    j = j + 1
    if (j == step) {
      val now = DateTime.now
      val delta = now.getMillis - low.getMillis
      val rate = step / (delta * 0.001)
      Logger.info(label.map(l => l + " ").getOrElse("") + nf.format(i) + total.map(t => " / " + t).getOrElse("") + " in " + delta + "ms @ " + nf.format(rate) + " per second")
      low = now
      j = 0
    }
    f
  }

}