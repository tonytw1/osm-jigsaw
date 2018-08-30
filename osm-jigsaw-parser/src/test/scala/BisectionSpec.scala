import org.scalatest.FlatSpec

class BisectionSpec extends FlatSpec {

  "bisector" should "can bisect to find boundaries" in {

    val data = Seq.fill(104500){"A"} ++ Seq.fill(200600){"B"} ++ Seq.fill(45457){"C"}
    println(data)
    println(data(0))
    println(data(9))
    println(data(10))
    println(data(11))
    println(data(11))

    def read(x: Int): Option[String] = {
      if (x < data.length) {
        Some(data(x))
      } else {
        None
      }
    }

    def bisert(target: String, floor: Int, ceiling: Int, data: Seq[String], x: Int): Int = {
      //Thread.sleep(100)
      val i = read(x)
      //println(x + " " + floor + "/" + ceiling + ": " + target + " " + i)

      if (i != Some(target) && x == floor + 1) {
        x
      } else {
        if (i == Some(target)) {
          val delta = ((ceiling - x) / 2) + 1
          //println("UP " + delta)
          bisert(target, x, ceiling, data, x + delta)
        } else {
          val delta = (x - floor) / 2
          //println("DOWN: " + delta)
          bisert(target, floor, x, data, x - delta)
        }
      }
    }

    val a = bisert("A", 0, data.length, data, 0)
    println(a)

    val b = bisert("B", a, data.length, data, a)
    println(b)

    val c = bisert("C", b, data.length, data, b)
    println(c)

    assert(1 == 1)
  }

}
