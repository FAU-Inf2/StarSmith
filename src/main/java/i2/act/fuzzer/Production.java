package i2.act.fuzzer;

import i2.act.gengraph.GeneratorGraphNode.ProductionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class Production implements ProductionNode {

  // for generator productions the id corresponds to the index of the generator value in the list
  // returned by the generator call, i.e., if the call returns the list ["foo", "bar", "fuzz"], the
  // productions for "foo", "bar", and "fuzz" have the ids 0, 1, and 2, respectively
  public final int id;
  public final String name;

  private final Class ownClass;
  private final List<Class> childClasses;

  private final int[] childVisitationOrder;

  public final boolean isListRecursion;

  public final int precedence;

  // only used for generator productions
  public final Object generatorValue;
  
  private final List<AttributeRule> attributeRules;

  public int minHeight;
  public int minSize;

  private boolean isRecursive;

  public int weight;

  // constructor for tree productions
  public Production(final int id, final String name, final int weight, final Class ownClass,
      final Class[] childClasses, final int[] childVisitationOrder, final boolean isListRecursion,
      final int precedence) {
    this(id, name, weight, ownClass, childClasses, childVisitationOrder,
        isListRecursion, precedence, null, new ArrayList<AttributeRule>());
  }

  // constructor for generator productions
  public Production(final int index, final String name, final int weight, final Class ownClass,
      final int precedence, final Object generatorValue, final List<AttributeRule> attributeRules) {
    this(index, name, weight, ownClass, new Class[] {}, new int[] {}, false, precedence,
        generatorValue, attributeRules);
  }

  private Production(final int id, final String name, final int weight, final Class ownClass,
      final Class[] childClasses, final int[] childVisitationOrder, final boolean isListRecursion,
      final int precedence, final Object generatorValue, final List<AttributeRule> attributeRules) {
    this.id = id;

    this.name = name;

    this.weight = weight;

    this.ownClass = ownClass;
    this.childClasses = Collections.unmodifiableList(java.util.Arrays.asList(childClasses));
    
    this.childVisitationOrder = childVisitationOrder;

    this.precedence = precedence;

    this.isListRecursion = isListRecursion;

    this.generatorValue = generatorValue;

    this.attributeRules = attributeRules;
  }

  public static final Production createLiteralProduction(final Class class_, final String literal,
      final int id) {
    assert (class_.isLiteralClass());

    final Production production =
        new Production(id, literal, 1, class_, new Class[0], new int[0], false, -1) {
      
      @Override
      public final Node[] createChildrenFor(final Node node, final int maxRecursionDepth) {
        return new Node[] {};
      }

      @Override
      public final void printCode(final Node node, final StringBuilder builder,
          final int indentation) {
        builder.append(literal);
      }

      @Override
      public final void tokenize(final Node node, final List<String> tokens) {
        tokens.add(literal);
      }

      @Override
      public final boolean isGeneratorNode() {
        return false;
      }

    };

    class_.addProduction(production);

    assert (class_.getAttributes().size() == 1);
    final Attribute strAttribute = class_.getAttributes().get(0);

    production.addAttributeRule(
        new AttributeRule(strAttribute, -1, new Attribute[] {}, new int[] {}) {

            @Override
            public final boolean alreadyComputed(final Node node) {
              return strAttribute.hasValue(node);
            }

            public final boolean allSourceAttributesAvailable(final Node node) {
              return true;
            }

            public final void compute(final Node node) {
              strAttribute.setValue(node, literal);
            }

        });

    return production;
  }

  @Override
  public final List<Class> predecessors() {
    return Arrays.asList(this.ownClass);
  }

  @Override
  public final List<Class> successors() {
    return this.childClasses;
  }

  @Override
  public final boolean isLiteralNode() {
    return this.ownClass.isLiteralClass();
  }

  @Override
  public final void setMinHeight(final int minHeight) {
    this.minHeight = minHeight;
  }

  @Override
  public final int getMinHeight() {
    return this.minHeight;
  }

  @Override
  public final void setMinSize(final int minSize) {
    this.minSize = minSize;
  }

  @Override
  public final int getMinSize() {
    return this.minSize;
  }

  @Override
  public final void setIsRecursive(final boolean recursive) {
    this.isRecursive = recursive;
  }

  @Override
  public final boolean isRecursive() {
    return this.isRecursive;
  }

  @Override
  public final String toString() {
    if (this.generatorValue == null) {
      return String.format("%s::%s", this.name, this.ownClass.name);
    } else {
      return String.format("%s::%s(%s%d)", this.name, this.ownClass.name, this.generatorValue,
          System.identityHashCode(this.generatorValue));
    }
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof Production)) {
      return false;
    }

    // XXX NOTE: this assumes that the reference of the generator value does not change!
    final Production otherProduction = (Production) other;
    return this.id == otherProduction.id
        && this.ownClass.equals(otherProduction.ownClass)
        && this.generatorValue == otherProduction.generatorValue;
  }

  @Override
  public final int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = (result * PRIME) + this.id;
    result = (result * PRIME) + this.ownClass.hashCode();
    result = (result * PRIME) + System.identityHashCode(this.generatorValue);
    return result;
  }

  @Override
  public final String getName(final boolean qualified) {
    if (qualified) {
      return String.format("%s::%s", this.ownClass.getName(true), this.name);
    } else {
      return this.name;
    }
  }

  public final Class ownClass() {
    return this.ownClass;
  }

  @Override
  public final ClassNode ownClassNode() {
    return ownClass();
  }

  public final List<Class> childClasses() {
    return this.childClasses;
  }

  public final int getNumberOfChildren() {
    return this.childClasses.size();
  }

  public final int[] getChildVisitationOrder() {
    return this.childVisitationOrder;
  }

  public final List<AttributeRule> getAttributeRules() {
    return this.attributeRules;
  }

  public final void addAttributeRule(final AttributeRule attributeRule) {
    this.attributeRules.add(attributeRule);
  }

  public final AttributeRule getAttributeRuleFor(final Attribute attribute,
      final int targetNodeIndex) {
    AttributeRule matchingAttributeRule = null;

    for (final AttributeRule attributeRule : this.attributeRules) {
      if ((targetNodeIndex == attributeRule.getTargetNodeIndex())
          && attributeRule.computesAttributeValue(attribute)) {
        assert (matchingAttributeRule == null) :
            "more than one matching attribute rule in prodution " + getName();
        matchingAttributeRule = attributeRule;
      }
    }

    assert (matchingAttributeRule != null)
        : "did not find a matching attribute rule in production " + getName();

    return matchingAttributeRule;
  }

  protected final int allowedHeight(final Node parentNode, final Class childClass,
      final int maxRecursionDepth) {
    final int maxHeight = childClass.getMaxHeight();

    if (!this.isRecursive) {
      return maxHeight;
    }

    final int parentAllowedHeight = parentNode.getAllowedHeight();

    final int allowedHeight;

    // recursive production
    if (parentAllowedHeight == -1) {
      // parent node is not recursive -> use maximum height for recursive sub-trees

      allowedHeight = maxRecursionDepth;
    } else {
      // parent is already part of a recursive sub-tree
      // -> decrement allowed height by 1 (except for lists)

      final Class parentClass = parentNode.getNodeClass();
      if (parentClass.isListClass() && parentClass == childClass) {
        allowedHeight = parentAllowedHeight;
      } else {
        allowedHeight = parentAllowedHeight - 1;
      }
    }

    if (maxHeight == -1) {
      return allowedHeight;
    } else {
      return Math.min(allowedHeight, maxHeight);
    }
  }

  protected final int allowedWidth(final Node parentNode, final Class childClass) {
    if (childClass.getMaxWidth() == -1) {
      // child class does not have a child limit
      return -1;
    }

    final Class parentClass = parentNode.getNodeClass();
    if (parentClass == childClass) {
      final int parentAllowedWidth = parentNode.getAllowedWidth();
      final int allowedWidth = parentAllowedWidth - 1;
      assert (allowedWidth >= 0);
      return allowedWidth;
    } else {
      return childClass.getMaxWidth();
    }
  }


  protected final void printIndentation(final StringBuilder builder, final int indentation) {
    for (int i = 0; i < indentation; ++i) {
      builder.append("  ");
    }
  }


  // --- to implement in the sub-classes ---


  public abstract Node[] createChildrenFor(final Node node, final int maxRecursionDepth);

  public abstract void printCode(final Node node, final StringBuilder builder,
      final int indentation);

  public abstract void tokenize(final Node node, final List<String> tokens);

}
