package progress

import org.apache.logging.log4j.scala.Logging

class ProgressCounter(step: Int, total: Option[Long] = None, label: Option[String] = None) extends Logging with CommaFormattedNumbers {

  var i = 0L
  var j = 0
  var start = System.currentTimeMillis()

  private val defaultMessage: (Long, Option[Long], Long, Double) => String = (i: Long, total: Option[Long], delta: Long, rate: Double) => {
    "Processed " + i + total.map(t => " / " + t).getOrElse("") + " in " + delta + "ms at " + rate + " per second"
  }

  def withProgress[R](f: => R, m: (Long, Option[Long], Long, Double) => String = defaultMessage): R = {
    i = i + 1
    j = j + 1
    if (j == step) {
      val now = System.currentTimeMillis()
      val delta = now - start
      val rate = step / (delta * 0.001)
      logger.info(m(i, total, delta, rate))
      start = now
      j = 0
    }
    f
  }

}