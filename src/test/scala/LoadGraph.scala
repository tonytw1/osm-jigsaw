package geocoding

import java.io.{FileInputStream, ObjectInputStream}

import model.GraphNode

trait LoadGraph {

  def loadGraph(graphFile: String): GraphNode = {
    println("Loading graph")
    val ois = new ObjectInputStream(new FileInputStream(graphFile))
    val head = ois.readObject.asInstanceOf[GraphNode]
    ois.close
    println("Loaded graph")
    head
  }

}
