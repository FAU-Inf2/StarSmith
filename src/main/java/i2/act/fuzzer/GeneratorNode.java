package i2.act.fuzzer;

import i2.act.fuzzer.runtime.Printable;

import java.util.ArrayList;
import java.util.List;

public abstract class GeneratorNode extends Node {

  public static final String GENERATOR_PRODUCTION_PREFIX = "__gen_";

  private List<Production> possibleProductions;

  public GeneratorNode(final Node parent, final int allowedHeight, final int allowedWidth,
      final Node expected) {
    super(parent, allowedHeight, allowedWidth, expected);
    this.possibleProductions = null; // will be computed on demand
  }

  public GeneratorNode(final Node parent, final int allowedHeight, final int allowedWidth,
      final Node expected, final int id, final Production production) {
    super(parent, allowedHeight, allowedWidth, expected, id, production);
    this.possibleProductions = null; // will be computed on demand
  }

  public final void clearPossibleProductions() {
    assert (!isResolved());
    this.possibleProductions = null;
  }

  @Override
  public final List<Production> getAvailableProductions() {
    if (this.possibleProductions == null) {
      assert (allInheritedAttributesEvaluated());

      this.possibleProductions = new ArrayList<Production>();

      final Class nodeClass = getNodeClass();

      int index = 0;
      final List<?> generatorValues = generatorValues(this);
      for (final Object generatorValue : generatorValues) {
        this.possibleProductions.add(new Production(index, GENERATOR_PRODUCTION_PREFIX + index, 1,
            nodeClass, nodeClass.getGeneratorPrecedence(), generatorValue,
            nodeClass.getGeneratorAttributeRules()) {

            @Override
            public final Node[] createChildrenFor(final Node node, final int maxRecursionDepth) {
              return new Node[] {}; // nodes of generator classes do not have any children 
            }

            @Override
            public final void printCode(final Node node, final StringBuilder builder,
                final int indentation) {
              if (this.generatorValue instanceof Printable) {
                builder.append(((Printable) this.generatorValue).print());
              } else {
                builder.append(this.generatorValue);
              }
            }

            @Override
            public final void tokenize(final Node node, final List<String> tokens) {
              if (this.generatorValue instanceof Printable) {
                tokens.add(((Printable) this.generatorValue).print());
              } else {
                tokens.add(String.valueOf(this.generatorValue));
              }
            }

            @Override
            public final boolean isGeneratorNode() {
              return true;
            }

        });

        ++index;
      }
    }

    return this.possibleProductions;
  }


  // --- to implement in the sub-classes ---

  @Override
  public abstract Class getNodeClass();

  @Override
  public abstract void clearAttributeValues(final boolean recursive);

  @Override
  public abstract void clearNonInheritedAttributeValues();

  @Override
  public abstract boolean allInheritedAttributesEvaluated();

  @Override
  public abstract boolean someSynthesizedAttributesEvaluated();

  @Override
  public abstract boolean allGuardsSatisfied(final boolean checkChildren);

  @Override
  public abstract boolean noGuardsFailing(final boolean checkChildren);

  @Override
  public abstract boolean allInheritedAttributesMatch(final Node otherNode);

  public abstract List<?> generatorValues(final Node node);

}
