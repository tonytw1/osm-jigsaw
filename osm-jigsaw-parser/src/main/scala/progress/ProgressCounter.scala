package progress

import org.apache.logging.log4j.scala.Logging

class ProgressCounter(step: Int, total: Option[Long] = None, label: Option[String] = None) extends Logging with CommaFormattedNumbers {

  var i = 0L
  var j = 0
  var start = System.currentTimeMillis()

  def withProgress[R](f: => R): R = {
    i = i + 1
    j = j + 1
    if (j == step) {
      val now = System.currentTimeMillis()
      val delta = now - start
      val rate = step / (delta * 0.001)

      logger.info(label.map(l => l + " ").getOrElse("") + commaFormatted(i) + total.map(t => " / " + commaFormatted(t)).getOrElse("") + " in " + delta + "ms @ " + nf.format(rate) + " per second")
      start = now
      j = 0
    }
    f
  }

}