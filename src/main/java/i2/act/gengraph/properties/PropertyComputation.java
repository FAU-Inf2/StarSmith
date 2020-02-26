package i2.act.gengraph.properties;

import i2.act.errors.RPGException;
import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.ast.LaLaSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class PropertyComputation<P> {

  public static enum Direction {
    FORWARDS,
    BACKWARDS;
  }

  private final Direction direction;

  public PropertyComputation(final Direction direction) {
    this.direction = direction;
  }

  protected abstract P init(final ClassNode _class, final GeneratorGraph generatorGraph);

  protected abstract P init(final ProductionNode production,
      final GeneratorGraph generatorGraph);

  protected P init(final GeneratorGraphNode graphNode, final GeneratorGraph generatorGraph) {
    if (graphNode instanceof ClassNode) {
      return init((ClassNode) graphNode, generatorGraph);
    } else {
      assert (graphNode instanceof ProductionNode);
      return init((ProductionNode) graphNode, generatorGraph);
    }
  }

  protected abstract P transfer(final ClassNode _class, final P in);

  protected abstract P transfer(final ProductionNode production, final P in);

  protected P transfer(final GeneratorGraphNode graphNode, final P in) {
    if (graphNode instanceof ClassNode) {
      return transfer((ClassNode) graphNode, in);
    } else {
      assert (graphNode instanceof ProductionNode);
      return transfer((ProductionNode) graphNode, in);
    }
  }

  protected abstract P confluence(final ClassNode _class, final Iterable<P> inSets);

  protected abstract P confluence(final ProductionNode production,
      final Iterable<P> inSets);

  protected P confluence(final GeneratorGraphNode graphNode, final Iterable<P> inSets) {
    if (graphNode instanceof ClassNode) {
      return confluence((ClassNode) graphNode, inSets);
    } else {
      assert (graphNode instanceof ProductionNode);
      return confluence((ProductionNode) graphNode, inSets);
    }
  }

  @SuppressWarnings("unchecked")
  private final Iterable<? extends GeneratorGraphNode> getInNodes(final GeneratorGraphNode node) {
    switch (this.direction) {
      case FORWARDS: {
        return node.predecessors();
      }
      case BACKWARDS: {
        return node.successors();
      }
      default: {
        throw new RPGException("unknown direction: " + this.direction);
      }
    }
  }

  private final Iterable<P> getInSets(final GeneratorGraphNode node,
      final Map<GeneratorGraphNode, P> properties) {
    final List<P> inSets = new ArrayList<>();

    final Iterable<? extends GeneratorGraphNode> inNodes = getInNodes(node);
    for (final GeneratorGraphNode inNode : inNodes) {
      assert (properties.containsKey(inNode)) : "no properties for " + inNode.getName();
      inSets.add(properties.get(inNode));
    }
    
    return inSets;
  }

  @SuppressWarnings("unchecked")
  protected Iterable<? extends GeneratorGraphNode> getOutNodes(final GeneratorGraphNode node) {
    switch (this.direction) {
      case FORWARDS: {
        return node.successors();
      }
      case BACKWARDS: {
        return node.predecessors();
      }
      default: {
        throw new RPGException("unknown direction: " + this.direction);
      }
    }
  }

  protected P handle(final GeneratorGraphNode node, final Map<GeneratorGraphNode, P> properties) {
    final Iterable<P> inSets = getInSets(node, properties);

    final P in = confluence(node, inSets);
    final P out = transfer(node, in);

    return out;
  }

  public final Map<GeneratorGraphNode, P> compute(final Specification specification) {
    return compute(GeneratorGraph.fromSpecification(specification));
  }

  public final Map<GeneratorGraphNode, P> compute(final LaLaSpecification specification) {
    return compute(GeneratorGraph.fromAST(specification));
  }

  @SuppressWarnings("unchecked")
  public final Map<GeneratorGraphNode, P> compute(final GeneratorGraph generatorGraph) {
    final Map<GeneratorGraphNode, P> properties = new HashMap<>();

    // initialize with empty sets
    for (final GeneratorGraphNode node : generatorGraph) {
      final P init = init(node, generatorGraph);
      properties.put(node, init);
    }

    // fix point computation
    final Queue<GeneratorGraphNode> worklist = new LinkedList<>();

    for (final GeneratorGraphNode graphNode : generatorGraph.getGraphNodes()) {
      // if a node does not have any "in nodes", its value cannot change after initialization
      // => only add nodes to worklist that have at least one "in node"
      // (this should handle unreachable classes correctly)
      if (getInNodes(graphNode).iterator().hasNext()) {
        worklist.add(graphNode);
      }
    }

    while (!worklist.isEmpty()) {
      final GeneratorGraphNode node = worklist.remove();

      final P out = handle(node, Collections.unmodifiableMap(properties));

      if (!out.equals(properties.get(node))) {
        properties.put(node, out);

        for (final GeneratorGraphNode outNode : getOutNodes(node)) {
          if (!worklist.contains(outNode)) {
            worklist.add(outNode);
          }
        }
      }
    }

    return properties;
  }

}
