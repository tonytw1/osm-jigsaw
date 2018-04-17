package resolving

trait NodeResolver {
  def resolvePointForNode(nodeId: Long): Option[(Double, Double)]
}

class InMemoryNodeResolver(nodes: Map[Long, (Double, Double)]) extends NodeResolver {

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    nodes.get(nodeId).map(n => (n._1, n._2))
  }

}
