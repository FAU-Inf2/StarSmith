package i2.act.util.graph;

import java.util.HashSet;
import java.util.Set;

public final class TransitiveReduction {
  
  public static final <V> void reduce(final Graph<V> graph) {
    for (final GraphNode<V> node : graph) {
      final Set<GraphNode<V>> reachableNodes = new HashSet<>();

      for (final GraphNode<V> successor : node.getSuccessors()) {
        final Set<GraphNode<V>> reachableNodesSuccessor = new HashSet<>();
        gatherNodes(successor, reachableNodesSuccessor);
        reachableNodesSuccessor.remove(successor);

        reachableNodes.addAll(reachableNodesSuccessor);
      }

      for (final GraphNode<V> reachableNode : reachableNodes) {
        graph.removeDirectedEdge(node, reachableNode);
      }
    }
  }

  private static final <V> void gatherNodes(final GraphNode<V> node,
      final Set<GraphNode<V>> reachableNodes) {
    reachableNodes.add(node);

    for (final GraphNode<V> successor : node.getSuccessors()) {
      if (!reachableNodes.contains(successor)) {
        gatherNodes(successor, reachableNodes);
      }
    }
  }

}
