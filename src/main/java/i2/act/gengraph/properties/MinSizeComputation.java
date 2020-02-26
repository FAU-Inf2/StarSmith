package i2.act.gengraph.properties;

import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.ast.LaLaSpecification;

import java.util.Map;

public final class MinSizeComputation extends PropertyComputation<Integer> {

  private static final int UNKNOWN = Integer.MAX_VALUE;

  private static final boolean PRINT_DEBUG_OUTPUT = false;

  private MinSizeComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  public static final Map<GeneratorGraphNode, Integer> computeMinSizes(
      final Specification specification) {
    return computeMinSizes(GeneratorGraph.fromSpecification(specification));
  }

  public static final Map<GeneratorGraphNode, Integer> computeMinSizes(
      final LaLaSpecification specification) {
    return computeMinSizes(GeneratorGraph.fromAST(specification));
  }

  public static final Map<GeneratorGraphNode, Integer> computeMinSizes(
      final GeneratorGraph generatorGraph) {
    final Map<GeneratorGraphNode, Integer> minSizes =
        (new MinSizeComputation()).compute(generatorGraph);

    for (final ClassNode classNode : generatorGraph.getClassNodes()) {
      assert (minSizes.containsKey(classNode));
      classNode.setMinSize(minSizes.get(classNode));

      for (final ProductionNode productionNode : classNode.getProductionNodes()) {
        assert (minSizes.containsKey(productionNode));
        productionNode.setMinSize(minSizes.get(productionNode));
      }
    }

    if (PRINT_DEBUG_OUTPUT) {
      for (final ClassNode classNode : generatorGraph.getClassNodes()) {
        System.err.format("=== %s: %d\n", classNode.getName(), minSizes.get(classNode));
        for (final ProductionNode productionNode : classNode.getProductionNodes()) {
          System.err.format("  %s: %d\n", productionNode.getName(), minSizes.get(productionNode));
        }
      }
    }

    return minSizes;
  }

  @Override
  protected final Integer init(final ClassNode _class, final GeneratorGraph generatorGraph) {
    if (_class.isLiteralNode() || _class.isGeneratorNode()) {
      return 1;
    } else {
      return UNKNOWN;
    }
  }

  @Override
  protected final Integer init(final ProductionNode production,
      final GeneratorGraph generatorGraph) {
    if (production.isLiteralNode() || production.isGeneratorNode()
        || production.isLeafProduction()) {
      return 1;
    } else {
      return UNKNOWN;
    }
  }

  @Override
  protected final Integer transfer(final ClassNode _class, final Integer in) {
    return in;
  }

  @Override
  protected final Integer transfer(final ProductionNode production, final Integer in) {
    return in;
  }

  @Override
  protected final Integer confluence(final ClassNode _class, final Iterable<Integer> inSets) {
    int min = UNKNOWN;

    for (final Integer in : inSets) {
      min = Math.min(min, in);
    }

    return min;
  }

  @Override
  protected final Integer confluence(final ProductionNode production,
      final Iterable<Integer> inSets) {
    int sum = 0;

    for (final Integer in : inSets) {
      if (in == UNKNOWN) {
        return UNKNOWN;
      }
      sum += in;
    }

    return sum + 1;
  }

}
