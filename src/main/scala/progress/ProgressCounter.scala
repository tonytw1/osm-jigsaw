package progress

import java.text.DecimalFormat

import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

class ProgressCounter(step: Int) extends Logging {
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
      logger.info(nf.format(i) + " in " + delta + "ms @ " + nf.format(rate) + " per second")
      low = now
      j = 0
    }
    f
  }

}