package i2.act.gengraph.properties;

import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class MinHeightComputation {

  private static final int UNKNOWN = Integer.MAX_VALUE;

  public static final void computeMinHeights(final Specification specification) {
    final GeneratorGraph generatorGraph = GeneratorGraph.fromSpecification(specification);
    computeMinHeights(generatorGraph);
    // generatorGraph.printMinHeights(); // just for debugging
  }

  public static final void computeMinHeights(final GeneratorGraph generatorGraph) {
    final Queue<ProductionNode> worklist = new LinkedList<>();

    // initialize min heights and worklist
    {
      for (final ClassNode classNode : generatorGraph.getClassNodes()) {
        if (!classNode.isLiteralNode() && !classNode.isGeneratorNode()) {
          classNode.setMinHeight(MinHeightComputation.UNKNOWN);

          for (final ProductionNode productionNode : classNode.getProductionNodes()) {
            productionNode.setMinHeight(MinHeightComputation.UNKNOWN);

            if (productionNode.isLeafProduction()) {
              worklist.add(productionNode);
            }
          }
        }
      }
    }

    // handle literal nodes and generator nodes
    {
      for (final ClassNode classNode : generatorGraph.getClassNodes()) {
        if (classNode.isLiteralNode() || classNode.isGeneratorNode()) {
          classNode.setMinHeight(1);

          for (final ProductionNode literalProductionNode : classNode.getProductionNodes()) {
            assert (literalProductionNode.isLeafProduction());
            literalProductionNode.setMinHeight(1);
          }

          updateWorklist(classNode, worklist);
        }
      }
    }

    while (!worklist.isEmpty()) {
      final ProductionNode productionNode = worklist.remove();

      final ClassNode productionClassNode = productionNode.ownClassNode();

      // compute min height of production
      {
        final boolean isList = productionClassNode.isListClass();

        int maxMinHeight = 0;
        for (final ClassNode childClassNode : productionNode.getChildClassNodes()) {
          final int minHeight;
          {
            if (isList && productionClassNode == childClassNode) {
              minHeight = childClassNode.getMinHeight() - 1;
            } else {
              minHeight = childClassNode.getMinHeight();
            }
          }

          if (minHeight > maxMinHeight) {
            maxMinHeight = minHeight;
          }
        }

        productionNode.setMinHeight(maxMinHeight + 1);
      }

      // update min height of class
      {
        assert (productionClassNode.getMinHeight() == MinHeightComputation.UNKNOWN
            || productionNode.getMinHeight() >= productionClassNode.getMinHeight());
        productionClassNode.setMinHeight(
            Math.min(productionClassNode.getMinHeight(), productionNode.getMinHeight()));
      }

      // update worklist
      updateWorklist(productionClassNode, worklist);
    }
  }

  private static final boolean allChildMinHeightsComputed(final ProductionNode productionNode) {
    for (final ClassNode childClassNode : productionNode.getChildClassNodes()) {
      if (childClassNode.getMinHeight() == MinHeightComputation.UNKNOWN) {
        return false;
      }
    }

    return true;
  }

  private static final void updateWorklist(final ClassNode classNode,
      final Queue<ProductionNode> worklist) {
    final List<? extends ProductionNode> generatingProductions =
        classNode.getGeneratingProductions();

    for (final ProductionNode enabledProduction : generatingProductions) {
      if (enabledProduction.getMinHeight() == MinHeightComputation.UNKNOWN
          && allChildMinHeightsComputed(enabledProduction)
          && !worklist.contains(enabledProduction)) {
        worklist.add(enabledProduction);
      }
    }
  }

}
