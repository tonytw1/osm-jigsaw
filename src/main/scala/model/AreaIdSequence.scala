package model

import java.util.concurrent.atomic.AtomicLong

object AreaIdSequence {

  val seq: AtomicLong = new AtomicLong(1L)

  def nextId: Long = {
    seq.getAndIncrement()
  }

}
