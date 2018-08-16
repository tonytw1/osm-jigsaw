package progress

import java.text.DecimalFormat

trait CommaFormattedNumbers {

  val nf = new DecimalFormat()

  def commaFormatted(i: Long) = {
    nf.format(i)
  }

}
