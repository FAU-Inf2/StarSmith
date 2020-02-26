package i2.act.gengraph.properties.dominance;

import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.gengraph.properties.PropertyComputation;
import i2.act.lala.ast.LaLaSpecification;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DominatorComputation extends PropertyComputation<Set<GeneratorGraphNode>> {

  private DominatorComputation() {
    super(PropertyComputation.Direction.FORWARDS);
  }

  public static DominatorTree computeDominators(final Specification specification) {
    return computeDominators(GeneratorGraph.fromSpecification(specification));
  }

  public static DominatorTree computeDominators(final LaLaSpecification specification) {
    return computeDominators(GeneratorGraph.fromAST(specification));
  }

  public static DominatorTree computeDominators(
      final GeneratorGraph generatorGraph) {
    final Map<GeneratorGraphNode, Set<GeneratorGraphNode>> dominators =
        (new DominatorComputation()).compute(generatorGraph);

    final DominatorTree dominatorTree =
        computeDominatorTree(generatorGraph.getRootNode(), dominators);

    return dominatorTree;
  }

  private static final DominatorTree computeDominatorTree(final GeneratorGraphNode rootNode,
      final Map<GeneratorGraphNode, Set<GeneratorGraphNode>> dominators) {
    final DominatorTree dominatorTree = new DominatorTree(rootNode);

    final Set<GeneratorGraphNode> nextNodes = new LinkedHashSet<>();
    nextNodes.add(rootNode);
    dominators.remove(rootNode);

    while (!nextNodes.isEmpty()) {
      final GeneratorGraphNode node = nextNodes.iterator().next();
      nextNodes.remove(node);

      for (final Map.Entry<GeneratorGraphNode, Set<GeneratorGraphNode>> entry
          : dominators.entrySet()) {
        final GeneratorGraphNode otherNode = entry.getKey();
        final Set<GeneratorGraphNode> remainingDominators = entry.getValue();

        remainingDominators.remove(node);

        if (remainingDominators.size() == 1) {
          // only one node left (the node itself)
          assert (remainingDominators.contains(otherNode));

          dominatorTree.addChild(node, otherNode);
          nextNodes.add(otherNode);
        }
      }

      for (final GeneratorGraphNode otherNode : nextNodes) {
        dominators.remove(otherNode);
      }
    }

    return dominatorTree;

  }

  @Override
  protected final Set<GeneratorGraphNode> init(final ClassNode _class,
      final GeneratorGraph generatorGraph) {
    if (_class == generatorGraph.getRootNode()) {
      // the only node that dominates the root node is the root node itself
      final Set<GeneratorGraphNode> rootDominators = new LinkedHashSet<GeneratorGraphNode>();
      rootDominators.add(_class);
      return rootDominators;
    } else {
      return new LinkedHashSet<GeneratorGraphNode>(generatorGraph.getGraphNodes());
    }
  }

  @Override
  protected final Set<GeneratorGraphNode> init(final ProductionNode production,
      final GeneratorGraph generatorGraph) {
    return new LinkedHashSet<GeneratorGraphNode>(generatorGraph.getGraphNodes());
  }

  @Override
  protected final Set<GeneratorGraphNode> transfer(final ClassNode _class,
      final Set<GeneratorGraphNode> in) {
    final Set<GeneratorGraphNode> out = new LinkedHashSet<>(in);
    out.add(_class);

    return out;
  }

  @Override
  protected final Set<GeneratorGraphNode> transfer(final ProductionNode production,
      final Set<GeneratorGraphNode> in) {
    final Set<GeneratorGraphNode> out = new LinkedHashSet<>(in);
    out.add(production);

    return out;
  }

  @Override
  protected final Set<GeneratorGraphNode> confluence(final ClassNode _class,
      final Iterable<Set<GeneratorGraphNode>> inSets) {
    assert (false); // handled by generic confluence() method
    return null;
  }

  @Override
  protected final Set<GeneratorGraphNode> confluence(final ProductionNode production,
      final Iterable<Set<GeneratorGraphNode>> inSets) {
    assert (false); // handled by generic confluence() method
    return null;
  }

  @Override
  protected final Set<GeneratorGraphNode> confluence(final GeneratorGraphNode node,
      final Iterable<Set<GeneratorGraphNode>> inSets) {
    Set<GeneratorGraphNode> out = null;

    for (final Set<GeneratorGraphNode> inSet : inSets) {
      if (out == null) {
        out = new LinkedHashSet<GeneratorGraphNode>(inSet);
      } else {
        out.retainAll(inSet);
      }
    }

    if (out == null) {
      return new LinkedHashSet<GeneratorGraphNode>();
    } else {
      return out;
    }
  }

}
