package input

import java.io.{BufferedInputStream, FileInputStream, InputStream}

import progress.ProgressCounter

trait ProtocolbufferReading {

  def processPbfFile[T](inputFilename: String, nextObject: InputStream => scala.Option[T], callback: T => Unit): Unit = {
    val fileInputStream = new BufferedInputStream(new FileInputStream(inputFilename))
    val counter = new ProgressCounter(step = 10000, label = Some("Reading"))
    var ok = true

    while (ok) {
      counter.withProgress {
        val item = nextObject(fileInputStream)
        item.map { i =>
          callback(i)
        }
        ok = item.nonEmpty
      }
    }

    fileInputStream.close()
  }

}
