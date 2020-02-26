package i2.act.fuzzer;

import i2.act.gengraph.GeneratorGraphNode;
import i2.act.gengraph.properties.MinSizeComputation;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RandomFuzzer extends Fuzzer {

  public static final class RandomProductionSelection implements Fuzzer.ProductionSelection {

    private final Random random;

    public RandomProductionSelection(final Random random) {
      this.random = random;
    }

    public final Production choose(final Node node, final List<Production> applicableProductions) {
      if (applicableProductions.isEmpty()) {
        throw new RuntimeException("no applicable production left");
      }

      if (applicableProductions.size() == 1) {
        return applicableProductions.get(0);
      }

      // for literal classes and generator classes all productions have the same weight
      if (node.getNodeClass().isLiteralClass() || node.getNodeClass().isGeneratorClass()) {
        return applicableProductions.get(this.random.nextInt(applicableProductions.size()));
      }

      int sumOfWeights = 0;
      {
        for (final Production production : applicableProductions) {
          sumOfWeights += production.weight;
        }
      }

      if (sumOfWeights == 0) {
        throw new RuntimeException("only productions of weight 0 left: " + applicableProductions);
      }

      final int random = this.random.nextInt(sumOfWeights);
      sumOfWeights = 0;

      for (final Production production : applicableProductions) {
        sumOfWeights += production.weight;
        if (random < sumOfWeights) {
          return production;
        }
      }

      assert (false);
      return null;
    }

  }

  public static final class SmallestProductionSelection implements Fuzzer.ProductionSelection {

    private final Random random;
    private final double smallProbability;
    private final RandomProductionSelection randomProductionSelection;

    private final Map<GeneratorGraphNode, Integer> minSizes;

    public SmallestProductionSelection(final Specification specification, final Random random,
        final double smallProbability) {
      this.random = random;
      this.smallProbability = smallProbability;
      this.randomProductionSelection = new RandomProductionSelection(random);

      if (smallProbability > 0.) {
        this.minSizes = MinSizeComputation.computeMinSizes(specification);
      } else {
        // no need to compute the min sizes if probability to choose small productions is 0
        this.minSizes = null;
      }
    }

    private final boolean chooseSmall() {
      return this.random.nextDouble() < this.smallProbability;
    }

    private final List<Production> getSmallestProductions(
        final List<Production> applicableProductions) {

      int minSize = Integer.MAX_VALUE;
      {
        for (final Production production : applicableProductions) {
          final int productionMinSize = this.minSizes.getOrDefault(production, 1);
          minSize = (productionMinSize < minSize) ? productionMinSize : minSize;
        }
      }
      assert (minSize < Integer.MAX_VALUE);

      final List<Production> smallestProductions = new ArrayList<>();
      {
        for (final Production production : applicableProductions) {
          final int productionMinSize = this.minSizes.getOrDefault(production, 1);
          if (productionMinSize <= minSize) {
            smallestProductions.add(production);
          }
        }
      }

      return smallestProductions;
    }

    public final Production choose(final Node node, final List<Production> applicableProductions) {
      if (applicableProductions.isEmpty()) {
        throw new RuntimeException("no applicable production left");
      }

      if (applicableProductions.size() == 1) {
        return applicableProductions.get(0);
      }

      // for literal classes and generator classes all productions have the same weight
      if (node.getNodeClass().isLiteralClass() || node.getNodeClass().isGeneratorClass()) {
        return applicableProductions.get(this.random.nextInt(applicableProductions.size()));
      }

      if (chooseSmall()) {
        final List<Production> smallestProductions = getSmallestProductions(applicableProductions);

        final Production chosenProduction = 
            this.randomProductionSelection.choose(node, smallestProductions);

        return chosenProduction;
      }

      return this.randomProductionSelection.choose(node, applicableProductions);
    }

  }

  private RandomFuzzer(final Specification specification,
      final Fuzzer.ProductionSelection productionSelection,
      final boolean syntaxOnly, final boolean restartOnFailure, final int timeout,
      final int maxAlternatives, final boolean debug, final BufferedWriter diagnosticsWriter,
      final BufferedWriter errorWriter) {
    super(specification, productionSelection, syntaxOnly, restartOnFailure, timeout,
        maxAlternatives, debug, diagnosticsWriter, errorWriter);
  }

  public static RandomFuzzer createFor(final Specification specification,
      final long randomSeed, final double smallProbability, final boolean syntaxOnly,
      final boolean restartOnFailure, final int timeout, final int maxAlternatives,
      final boolean debug, final BufferedWriter diagnosticsWriter,
      final BufferedWriter errorWriter) {
    final Random random = new Random(randomSeed);

    final Fuzzer.ProductionSelection productionSelection;
    {
      if (smallProbability > 0.) {
        productionSelection =
            new SmallestProductionSelection(specification, random, smallProbability);
      } else {
        productionSelection = new RandomProductionSelection(random);
      }
    }

    return new RandomFuzzer(
        specification, productionSelection, syntaxOnly, restartOnFailure, timeout, maxAlternatives,
        debug, diagnosticsWriter, errorWriter);
  }

}
