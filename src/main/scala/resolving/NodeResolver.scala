package resolving

class NodeResolver(nodes: Map[Long, (Double, Double)]) {

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    nodes.get(nodeId).map(n => (n._1, n._2))
  }

}
