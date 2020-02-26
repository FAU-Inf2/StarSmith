package i2.act.util.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class Kosaraju {

  public static final class SCC<V> implements Iterable<V> {

    private final Set<V> elements;

    public SCC() {
      this.elements = new LinkedHashSet<>();
    }

    public final void addElement(final V element) {
      this.elements.add(element);
    }

    public final Set<V> getElements() {
      return Collections.unmodifiableSet(this.elements);
    }

    public final boolean contains(final V element) {
      return this.elements.contains(element);
    }

    @Override
    public final Iterator<V> iterator() {
      return this.elements.iterator();
    }

    @Override
    public final String toString() {
      return this.elements.toString();
    }

  }
  
  public static final <V> Graph<SCC<V>> computeKernelDAG(final Graph<V> graph) {
    final Graph<SCC<V>> kernelDAG = new Graph<>();

    // compute post order numbers
    final Map<GraphNode<V>, Integer> nodeToNumber = new HashMap<>();
    final Map<Integer, GraphNode<V>> numberToNode = new HashMap<>();
    {
      int nextNumber = 0;

      for (final GraphNode<V> node : graph) {
        if (!nodeToNumber.containsKey(node)) {
          nextNumber = computePostOrderNumbers(node, nodeToNumber, numberToNode, nextNumber);
        }
      }
    }

    // compute strongly connected components
    final Map<GraphNode<V>, SCC<V>> components = new HashMap<>();
    {
      int nextStart = graph.size() - 1;
      while (nextStart >= 0) {
        final GraphNode<V> nextNode = numberToNode.get(nextStart);
        assert (nextNode != null) : nextStart;

        if (!components.containsKey(nextNode)) {
          final SCC<V> scc = new SCC<V>();
          computeSCC(nextNode, scc, nodeToNumber, components);

          kernelDAG.addNode(scc);
        }

        --nextStart;
      }
    }

    // compute kernel DAG
    {
      for (final GraphNode<V> from : graph) {
        final SCC<V> fromSCC = components.get(from);
        assert (fromSCC != null);

        for (final GraphNode<V> to : from.getSuccessors()) {
          final SCC<V> toSCC = components.get(to);
          assert (toSCC != null);

          if (fromSCC != toSCC) {
            kernelDAG.addDirectedEdge(fromSCC, toSCC);
          }
        }
      }
    }

    return kernelDAG;
  }

  private static final <V> int computePostOrderNumbers(final GraphNode<V> node,
      final Map<GraphNode<V>, Integer> nodeToNumber, final Map<Integer, GraphNode<V>> numberToNode,
      int nextNumber) {
    nodeToNumber.put(node, null); // mark as visited

    for (final GraphNode<V> successor : node.getSuccessors()) {
      if (!nodeToNumber.containsKey(successor)) {
        nextNumber = computePostOrderNumbers(successor, nodeToNumber, numberToNode, nextNumber);
      }
    }

    nodeToNumber.put(node, nextNumber);
    assert (!numberToNode.containsKey(nextNumber));
    numberToNode.put(nextNumber, node);

    return nextNumber + 1;
  }

  private static final <V> void computeSCC(final GraphNode<V> node, final SCC<V> scc,
      final Map<GraphNode<V>, Integer> nodeToNumber, final Map<GraphNode<V>, SCC<V>> components) {
    scc.addElement(node.getValue());
    components.put(node, scc);

    for (final GraphNode<V> predecessor : node.getPredecessors()) {
      if (!components.containsKey(predecessor)) {
        computeSCC(predecessor, scc, nodeToNumber, components);
      }
    }
  }

}
