package geocoding

import java.io.{FileInputStream, ObjectInputStream}

import model.GraphNode

trait LoadGraph {

  def loadGraph(graphFile: String): GraphNode = {
    val ois = new ObjectInputStream(new FileInputStream(graphFile))
    val head = ois.readObject.asInstanceOf[GraphNode]
    ois.close
    head
  }

}
