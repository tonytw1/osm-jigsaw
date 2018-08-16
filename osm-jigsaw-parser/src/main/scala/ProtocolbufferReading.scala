import java.io.{BufferedInputStream, FileInputStream, InputStream}

import progress.ProgressCounter

trait ProtocolbufferReading {

  def processPbfFile[T](inputFilename: String, nextObject: InputStream => scala.Option[T], callback: T => Unit): Unit = {
    val fileInputStream = new BufferedInputStream(new FileInputStream(inputFilename))
    val counter = new ProgressCounter(step = 100000, label = Some("Reading"))
    var ok = true

    while (ok) {
      counter.withProgress {
        val outputArea = nextObject(fileInputStream)
        outputArea.map { oa =>
          callback(oa)
        }
        ok = outputArea.nonEmpty
      }
    }

    fileInputStream.close()
  }

}
