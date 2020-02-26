package i2.act.fuzzer;

public abstract class AttributeRule {

  private final Attribute targetAttribute;
  private final int targetNodeIndex;

  private final Attribute[] sourceAttributes;
  private final int[] sourceNodeIndexes;

  private final boolean isInheritedRule;
  private final boolean isSynthesizedRule;
  private final boolean isGuardRule;
  private final boolean isGuardRuleOnInheritedAttributes;

  public AttributeRule(final Attribute targetAttribute, final int targetNodeIndex,
      final Attribute[] sourceAttributes, final int[] sourceNodeIndexes) {
    this.targetAttribute = targetAttribute;
    this.targetNodeIndex = targetNodeIndex;
    this.sourceAttributes = sourceAttributes;
    this.sourceNodeIndexes = sourceNodeIndexes;

    this.isInheritedRule = this.targetAttribute.isInherited();
    this.isSynthesizedRule = this.targetAttribute.isSynthesized();
    this.isGuardRule = this.targetAttribute.isGuard();
    this.isGuardRuleOnInheritedAttributes = checkIfGuardRuleOnInheritedAttributes();
  }

  private final boolean checkIfGuardRuleOnInheritedAttributes() {
    if (!this.isGuardRule) {
      return false;
    }

    for (final Attribute sourceAttribute : this.sourceAttributes) {
      if (!sourceAttribute.isInherited()) {
        return false;
      }
    }

    return true;
  }

  public final boolean isInheritedRule() {
    return this.isInheritedRule;
  }

  public final boolean isSynthesizedRule() {
    return this.isSynthesizedRule;
  }

  public final boolean isGuardRule() {
    return this.isGuardRule;
  }

  public final boolean isGuardRuleOnInheritedAttributes() {
    return this.isGuardRuleOnInheritedAttributes;
  }

  public final Attribute getTargetAttribute() {
    return this.targetAttribute;
  }

  public final int getTargetNodeIndex() {
    return this.targetNodeIndex;
  }

  public final Node getTargetNode(final Node node) {
    if (this.targetNodeIndex == -1) {
      return node;
    }
    return node.getChildren().get(this.targetNodeIndex);
  }

  public final Attribute[] getSourceAttributes() {
    return this.sourceAttributes;
  }

  public final int[] getSourceNodeIndexes() {
    return this.sourceNodeIndexes;
  }

  public final int getNumberOfSourceAttributes() {
    return this.sourceAttributes.length;
  }

  public final boolean computesAttributeValue(final Attribute attribute) {
    return this.targetAttribute == attribute;
  }

  public final boolean evaluate(final Node node, final boolean printExceptions) {
    if (!allSourceAttributesAvailable(node)) {
      return false;
    }

    try {
      compute(node);
      return true;
    } catch (final Exception exception) {
      if (printExceptions) {
        System.err.format("[i] unable to compute attribute '%s' of node %s: %s\n",
            this.targetAttribute.getName(), node.getNodeName(),
            exception.getMessage());
      }
      return this.targetAttribute.clearValue(getTargetNode(node));
    } catch (final AssertionError assertionError) {
      System.err.format("[!] computation of attribute '%s' of node %s triggered an assertion: %s\n",
          this.targetAttribute.getName(), node.getNodeName(),
          assertionError.getMessage());
      return this.targetAttribute.clearValue(getTargetNode(node));
    }
  }


  // --- to implement in the sub-classes ---


  public abstract boolean alreadyComputed(final Node node);

  public abstract boolean allSourceAttributesAvailable(final Node node);

  public abstract void compute(final Node node);

}
