package i2.act.util.graph;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GraphNode<V> {

  private static int idCounter;

  private final int id;

  private final V value;

  private final Set<GraphNode<V>> predecessors;
  private final Set<GraphNode<V>> successors;

  public GraphNode(final V value) {
    this.id = GraphNode.idCounter++;

    this.value = value;
    this.predecessors = new LinkedHashSet<GraphNode<V>>();
    this.successors = new LinkedHashSet<GraphNode<V>>();
  }

  public final int getId() {
    return this.id;
  }

  public final V getValue() {
    return this.value;
  }

  public final int getNumberOfSuccessors() {
    return this.successors.size();
  }

  public final int getNumberOfPredecessors() {
    return this.predecessors.size();
  }

  protected final boolean containsDirectedEdge(final GraphNode<V> to) {
    return this.successors.contains(to);
  }

  protected final boolean containsUndirectedEdge(final GraphNode<V> to) {
    return this.successors.contains(to) && to.successors.contains(this);
  }

  protected final void addDirectedEdge(final GraphNode<V> to) {
    this.successors.add(to);
    to.predecessors.add(this);
  }

  protected final void addUndirectedEdge(final GraphNode<V> to) {
    addDirectedEdge(to);
    to.addDirectedEdge(this);
  }

  protected final void removeDirectedEdge(final GraphNode<V> to) {
    this.successors.remove(to);
    to.predecessors.remove(this);
  }

  protected final void removeUndirectedEdge(final GraphNode<V> to) {
    removeDirectedEdge(to);
    to.removeDirectedEdge(this);
  }

  public final Set<GraphNode<V>> getPredecessors() {
    return Collections.unmodifiableSet(this.predecessors);
  }

  public final Set<GraphNode<V>> getSuccessors() {
    return Collections.unmodifiableSet(this.successors);
  }

}
