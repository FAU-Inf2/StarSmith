package i2.act.util.graph;

import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Graph<V> implements Iterable<GraphNode<V>> {

  public interface DotStyle<V> {

    public String label(final V value);

    public String style(final V value);

  }

  public final DotStyle<V> defaultDotStyle = new DotStyle<V>() {

    @Override
    public final String label(final V value) {
      return String.valueOf(value);
    }

    @Override
    public final String style(final V value) {
      return "";
    }

  };

  private final Map<V, GraphNode<V>> nodes;

  public Graph() {
    this.nodes = new LinkedHashMap<V, GraphNode<V>>();
  }

  public static final <V> Graph<V> from(final Map<V, Set<V>> edges) {
    final Graph<V> graph = new Graph<V>();

    for (final V from : edges.keySet()) {
      for (final V to : edges.get(from)) {
        graph.addDirectedEdge(from, to);
      }
    }

    return graph;
  }

  public final int size() {
    return this.nodes.size();
  }

  public final GraphNode<V> getNode(final V value) {
    return this.nodes.get(value);
  }

  public final Collection<GraphNode<V>> getGraphNodes() {
    return this.nodes.values();
  }

  public final Collection<V> getGraphNodeValues() {
    return this.nodes.keySet();
  }

  public final boolean containsNode(final V value) {
    return this.nodes.containsKey(value);
  }

  public final boolean containsDirectedEdge(final V from, final V to) {
    if (!(containsNode(from) && containsNode(to))) {
      return false;
    }

    final GraphNode<V> fromNode = addNode(from);
    final GraphNode<V> toNode = addNode(to);

    return containsDirectedEdge(fromNode, toNode);
  }

  public final boolean containsDirectedEdge(final GraphNode<V> fromNode,
      final GraphNode<V> toNode) {
    return fromNode.containsDirectedEdge(toNode);
  }

  public final boolean containsUndirectedEdge(final V from, final V to) {
    if (!(containsNode(from) && containsNode(to))) {
      return false;
    }

    final GraphNode<V> fromNode = addNode(from);
    final GraphNode<V> toNode = addNode(to);

    return containsUndirectedEdge(fromNode, toNode);
  }

  public final boolean containsUndirectedEdge(final GraphNode<V> fromNode,
      final GraphNode<V> toNode) {
    return fromNode.containsUndirectedEdge(toNode);
  }

  public final GraphNode<V> addNode(final V value) {
    if (this.nodes.containsKey(value)) {
      return this.nodes.get(value);
    }

    final GraphNode<V> node = new GraphNode<V>(value);
    this.nodes.put(value, node);

    return node;
  }

  public final void addDirectedEdge(final V from, final V to) {
    final GraphNode<V> fromNode = addNode(from);
    final GraphNode<V> toNode = addNode(to);

    addDirectedEdge(fromNode, toNode);
  }

  public final void addDirectedEdge(final GraphNode<V> fromNode, final GraphNode<V> toNode) {
    fromNode.addDirectedEdge(toNode);
  }

  public final void addUndirectedEdge(final V one, final V two) {
    final GraphNode<V> nodeOne = addNode(one);
    final GraphNode<V> nodeTwo = addNode(two);

    addUndirectedEdge(nodeOne, nodeTwo);
  }

  public final void addUndirectedEdge(final GraphNode<V> nodeOne, final GraphNode<V> nodeTwo) {
    nodeOne.addUndirectedEdge(nodeTwo);
  }

  public final void removeDirectedEdge(final V from, final V to) {
    if (!(containsNode(from) && containsNode(to))) {
      return;
    }

    final GraphNode<V> fromNode = addNode(from);
    final GraphNode<V> toNode = addNode(to);

    removeDirectedEdge(fromNode, toNode);
  }

  public final void removeDirectedEdge(final GraphNode<V> fromNode, final GraphNode<V> toNode) {
    fromNode.removeDirectedEdge(toNode);
  }

  public final void removeUndirectedEdge(final V one, final V two) {
    if (!(containsNode(one) && containsNode(two))) {
      return;
    }

    final GraphNode<V> nodeOne = addNode(one);
    final GraphNode<V> nodeTwo = addNode(two);

    removeUndirectedEdge(nodeOne, nodeTwo);
  }

  public final void removeUndirectedEdge(final GraphNode<V> nodeOne, final GraphNode<V> nodeTwo) {
    nodeOne.removeUndirectedEdge(nodeTwo);
  }

  public final void printDot(final BufferedWriter writer) {
    printDot(writer, this.defaultDotStyle);
  }

  public final void printDot(final BufferedWriter writer, final DotStyle<V> dotStyle) {
    FileUtil.write("digraph G {\n", writer);

    printDotBody("", writer, dotStyle);

    FileUtil.write("}", writer);
    FileUtil.flushWriter(writer);
  }

  public final void printDotBody(final String nodePrefix, final BufferedWriter writer,
      final DotStyle<V> dotStyle) {
    final Map<GraphNode<V>, String> nodeNames = new HashMap<>();

    // nodes
    for (final GraphNode<V> node : this.nodes.values()) {
      final String nodeName = String.format("n%s_%d", nodePrefix, node.getId());
      final String nodeLabel = dotStyle.label(node.getValue());
      final String nodeStyle = dotStyle.style(node.getValue());

      FileUtil.write(
          String.format("  %s [label=\"%s\", %s];\n", nodeName, nodeLabel, nodeStyle), writer);
      nodeNames.put(node, nodeName);
    }

    // edges
    for (final GraphNode<V> node : this.nodes.values()) {
      final String fromName = nodeNames.get(node);
      assert (fromName != null);

      for (final GraphNode<V> successor : node.getSuccessors()) {
        final String toName = nodeNames.get(successor);
        assert (toName != null);

        FileUtil.write(String.format("  %s -> %s;\n", fromName, toName), writer);
      }
    }
  }

  @Override
  public final Iterator<GraphNode<V>> iterator() {
    return this.nodes.values().iterator();
  }

}
