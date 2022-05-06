package i2.act.fuzzer;

import i2.act.util.Pair;
import i2.act.util.lexer.Lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Node {

  private static int idCounter;

  public static final void resetIdCounter() {
    Node.idCounter = 0;
  }
  
  // ===============================================================================================

  public int id;
  
  protected Node parent;
  protected List<Node> children;

  protected int allowedHeight;
  protected int allowedWidth;

  public Node expected;

  protected Production production;

  protected List<FailPattern> failPatterns;

  // default constructor
  public Node(final Node parent, final int allowedHeight, final int allowedWidth,
      final Node expected) {
    this(parent, allowedHeight, allowedWidth, expected,
        (expected == null) ? (++Node.idCounter) : expected.id,
        null);
  }

  // constructor for cloning
  protected Node(final Node parent, final int allowedHeight, final int allowedWidth,
      final Node expected, final int id, final Production production) {
    this.id = id;

    this.parent = parent;
    this.production = production;
    this.children = new ArrayList<Node>();
    this.allowedHeight = allowedHeight;
    this.allowedWidth = allowedWidth;
    this.expected = expected;

    this.failPatterns = new ArrayList<FailPattern>();
  }

  public final Node getParent() {
    return this.parent;
  }

  public final void setParent(final Node parent) {
    this.parent = parent;
  }

  public final List<Node> getChildren() {
    return this.children;
  }

  public final Node getChild(final int childIndex) {
    return this.children.get(childIndex);
  }

  public final int getNumberOfChildren() {
    return this.children.size();
  }

  public final int getIndexOfChild(final Node child) {
    int index = 0;
    for (final Node otherChild : this.children) {
      if (otherChild == child) {
        return index;
      }
      ++index;
    }

    return -1;
  }

  public final Production getProduction() {
    return this.production;
  }

  public final boolean isResolved() {
    return this.production != null;
  }

  public final void applyProduction(final Production production, final int maxRecursionDepth) {
    this.production = production;
    this.children = new ArrayList<Node>(
        java.util.Arrays.asList(production.createChildrenFor(this, maxRecursionDepth)));

    // set 'expected' subtrees in children
    if (this.expected != null && this.expected.production != null) {
      if (production == this.expected.production) {
        final int numberOfChildren = this.children.size();
        assert (numberOfChildren == this.expected.children.size());
        for (int index = 0; index < numberOfChildren; ++index) {
          final Node child = this.children.get(index);
          final Node expectedChild = this.expected.children.get(index);

          child.expected = expectedChild;
          child.id = expectedChild.id;
        }
      } else {
        System.err.format("[i] production '%s' does not match expected production '%s'\n",
            production, this.expected.production);
      }
    }
  }

  public final int getAllowedHeight() {
    return this.allowedHeight;
  }

  public final int getAllowedWidth() {
    return this.allowedWidth;
  }

  public final List<FailPattern> getFailPatterns() {
    return this.failPatterns;
  }

  public final void addFailPattern(final FailPattern failPattern) {
    this.failPatterns.add(failPattern);
  }

  public final void setFailPatterns(final List<FailPattern> failPatterns) {
    this.failPatterns.clear();
    this.failPatterns.addAll(failPatterns);
  }

  public final void clearFailPatterns() {
    this.failPatterns.clear();
  }

  public final void evaluateAttributesLoop() {
    evaluateAttributesLoop(false, false);
  }

  public final void evaluateAttributesLoop(final boolean shallow) {
    evaluateAttributesLoop(shallow, false);
  }

  public final void evaluateAttributesLoop(final boolean shallow,
      final boolean printExceptions) {
    int round = 0;
    while (evaluateAttributes(round == 0, shallow, printExceptions)) {
      ++round;
    }
  }

  public final boolean evaluateAttributes(boolean firstRound, final boolean shallow,
      final boolean printExceptions) {
    boolean change = false;

    boolean changeThisRound = true;
    while (changeThisRound) {
      changeThisRound = false;

      final Pair<Boolean, List<Node>> ownEvaluation = evaluateOwnAttributes(printExceptions);
      final boolean ownRuleEvaluated = ownEvaluation.getFirst();
      final List<Node> changedChildren = ownEvaluation.getSecond();

      changeThisRound |= ownRuleEvaluated;
      change |= changeThisRound;

      final List<Node> childrenToVisit;
      {
        if (firstRound) {
          childrenToVisit = this.children;
        } else {
          childrenToVisit = changedChildren;
        }
      }

      for (final Node child : childrenToVisit) {
        if (!shallow || !child.isGuardedOrUnit()) {
          changeThisRound |= child.evaluateAttributes(firstRound, shallow, printExceptions);
        }
      }

      firstRound = false;
    }
    
    return change;
  }

  private final Pair<Boolean, List<Node>> evaluateOwnAttributes(final boolean printExceptions) {
    boolean someRuleEvaluated = false;
    final List<Node> changedChildren = new ArrayList<>();

    if (this.production != null) {
      final boolean allGuardsSatisfied = allGuardsSatisfied(false);

      final List<AttributeRule> attributeRules = this.production.getAttributeRules();
      for (final AttributeRule attributeRule : attributeRules) {
        if (attributeRule.alreadyComputed(this)) {
          // attribute value has already been computed => do nothing
        } else if (attributeRule.isSynthesizedRule() && !allGuardsSatisfied) {
          // only evaluate synthesized attribute if all guards evaluate successfully
        } else {
          final boolean ruleEvaluated = attributeRule.evaluate(this, printExceptions);

          if (ruleEvaluated) {
            someRuleEvaluated = true;
            if (attributeRule.isInheritedRule()) {
              final Node targetNode = attributeRule.getTargetNode(this);
              changedChildren.add(targetNode);
            }
          }
        }
      }
    }

    return new Pair<>(someRuleEvaluated, changedChildren);
  }

  public final int size() {
    int size = 1;

    for (final Node child : this.children) {
      size += child.size();
    }

    return size;
  }

  public final int depth() {
    int maxDepth = 0;

    for (final Node child : this.children) {
      final int childDepth = child.depth();
      if (childDepth > maxDepth) {
        maxDepth = childDepth;
      }
    }

    if (getNodeClass().isList
        && this.parent != null && this.parent.getNodeClass() == getNodeClass()) {
      return maxDepth;
    } else {
      return maxDepth + 1;
    }
  }

  public final int numberOfAttributes() {
    int numberOfAttributes = this.getNodeClass().getAttributes().size();

    for (final Node child : this.children) {
      numberOfAttributes += child.numberOfAttributes();
    }

    return numberOfAttributes;
  }

  public final String getNodeName() {
    return String.format("<!%s<%x>!>", this.getNodeClass().getName(), hashCode());
  }

  public final String printCode() {
    return printCode(0);
  }

  public final String printCode(final int indentation) {
    return printCode(new StringBuilder(), indentation).toString();
  }

  public final StringBuilder printCode(final StringBuilder builder, final int indentation) {
    if (this.production == null) {
      builder.append(getNodeName());
    } else {
      this.production.printCode(this, builder, indentation);
    }

    return builder;
  }

  public final List<String> tokenize() {
    final List<String> tokens = new ArrayList<>();
    tokenize(tokens);

    return tokens;
  }

  public final void tokenize(final List<String> tokens) {
    if (this.production != null) {
      this.production.tokenize(this, tokens);
    }
  }

  public final String printTree() {
    return printTree(true);
  }

  public final String printTree(final boolean printAttributeValues) {
    return printTreeHelper(new StringBuilder(), printAttributeValues, 0).toString();
  }

  private final StringBuilder printTreeHelper(final StringBuilder builder,
      final boolean printAttributeValues, final int indentation) {
    for (int i = 0; i < indentation; ++i) {
      builder.append("  ");
    }
    builder.append(this.getNodeClass().getName());

    builder.append(String.format("<%x>{%d}", this.hashCode(), this.allowedHeight));

    if (printAttributeValues) {
      builder.append("(");

      final List<Attribute> attributes = this.getNodeClass().getAttributes();

      boolean first = true;
      for (final Attribute attribute : attributes) {
        if (attribute.hasValue(this)) {
          if (first) {
            first = false;
          } else {
            builder.append(", ");
          }

          builder.append(attribute.getName());
          builder.append(": ");
          builder.append(attribute.getValue(this));
        }
      }

      builder.append(")");
    }

    if (this.production != null) {
      builder.append(" -> ");
      builder.append(this.production.getName());
    }

    builder.append("\n");

    for (final Node child : this.children) {
      child.printTreeHelper(builder, printAttributeValues, indentation + 1);
    }
    
    return builder;
  }

  private final String getProductionName() {
    if (this.production == null) {
      return "?";
    } else {
      return this.production.name;
    }
  }

  private final String getProductionId() {
    if (this.production == null) {
      return "?";
    } else {
      return String.valueOf(this.production.id);
    }
  }

  public final String serialize(final boolean shortFormat) {
    return serialize(new StringBuilder(), shortFormat).toString();
  }

  private final StringBuilder serialize(final StringBuilder builder, final boolean shortFormat) {
    builder.append("(");

    if (shortFormat) {
      builder.append(String.format("%d:%d:%s", this.id, getNodeClass().id, getProductionId()));
    } else {
      builder.append(String.format("%d:%s:%s", this.id, getNodeClass().name,
          getSerializedProductionName()));
    }

    for (final Node child : this.children) {
      child.serialize(builder, shortFormat);
    }

    builder.append(")");

    return builder;
  }

  private final String getSerializedProductionName() {
    if (this.production == null) {
      return "?";
    } else {
      if (getNodeClass().isLiteralClass()) {
        return String.format("%s%s%s", Lexer.ESCAPED_IDENTIFIER_DELIMITER, this.production.name,
            Lexer.ESCAPED_IDENTIFIER_DELIMITER);
      } else {
        return this.production.name;
      }
    }
  }

  public final String toDot(final boolean includeIDs) {
    final StringBuilder builder = new StringBuilder();

    builder.append("digraph G {\n");

    builder.append("\tgraph [ordering=\"out\"];\n");
    builder.append("\tnode [shape=box, style=filled, fillcolor=white,"
        + " fontname=\"Droid Sans Mono\"];\n");

    // the actual node ids are not guaranteed to be unique...
    Node.dotIDCounter = 0;

    toDot(builder, includeIDs);

    builder.append("}");

    return builder.toString();
  }

  private static int dotIDCounter;

  private final int toDot(final StringBuilder builder, final boolean includeIDs) {
    // the actual node ids are not guaranteed to be unique...
    final int id = Node.dotIDCounter++;

    String format = "";
    {
      if (isGuardedOrUnit()) {
        format += ", fillcolor=gray78";
      }

      if (!isResolved()) {
        format += ", shape=ellipse";
      }
    }

    final String label;
    {
      if (includeIDs) {
        label = String.format("%d:%s", this.id, getProductionName());
      } else {
        label = getProductionName();
      }
    }

    builder.append(String.format("\tn%d [label=\"%s\"%s];\n", id, label, format));

    for (final Node child : this.children) {
      final int childID = child.toDot(builder, includeIDs);
      builder.append(String.format("\tn%d -> n%d;\n", id, childID));
    }

    return id;
  }

  public final boolean subtreeFinished() {
    if (!isResolved()) {
      return false;
    }

    for (final Node child : this.children) {
      if (!child.subtreeFinished()) {
        return false;
      }
    }

    return true;
  }

  private static final boolean isGuardedOrUnit(final Class _class) {
    return _class.hasGuardAttribute() || _class.isUnit;
  }

  public final boolean isGuardedOrUnit() {
    return isGuardedOrUnit(getNodeClass());
  }

  public final void deconstruct() {
    this.production = null;

    // make sure that 'parent' references in children are consistent
    for (final Node child : this.children) {
      child.parent = null;
    }

    this.children.clear();

    clearAttributeValues(false);
  }

  public final void deconstructButKeepInheritedAttributeValues() {
    this.production = null;
    this.children.clear();
    clearNonInheritedAttributeValues();
  }

  public final boolean allGuardsOnInheritedAttributesSatisfied() {
    assert (isResolved());

    for (final AttributeRule attributeRule : this.production.getAttributeRules()) {
      if (attributeRule.isGuardRuleOnInheritedAttributes()) {
        final Attribute guardAttribute = attributeRule.getTargetAttribute();

        if (guardAttribute.hasValue(this) && !((boolean) guardAttribute.getValue(this))) {
          return false;
        }
      }
    }

    return true;
  }

  // will be overridden in sub-classes
  public void replaceBy(final Node otherNode, final boolean replaceFailPatterns) {
    this.production = otherNode.production;
    this.allowedHeight = otherNode.allowedHeight;
    this.allowedWidth = otherNode.allowedWidth;

    this.children = otherNode.children;
    for (final Node child : this.children) {
      child.parent = this;
    }

    if (replaceFailPatterns) {
      this.failPatterns = otherNode.failPatterns;
    }
  }

  public final void replaceChild(final int childIndex, final Node newChild) {
    assert (childIndex >= 0 && childIndex < this.children.size());
    assert (newChild.getNodeClass() == this.children.get(childIndex).getNodeClass()) :
        String.format("%s != %s", newChild.getNodeClass().getName(),
            this.children.get(childIndex).getNodeClass().getName());

    this.children.set(childIndex, newChild);
  }

  public final void replaceChild(final Node oldChild, final Node newChild) {
    this.children.set(getIndexOfChild(oldChild), newChild);
  }

  @Override
  public final String toString() {
    return printCode();
  }

  public final List<Production> getPossibleProductions() {
    if (this.expected != null && this.expected.production != null) {
      return Arrays.asList(this.expected.production);
    } else {
      return getAvailableProductions();
    }
  }

  public final Node cloneTree(final Node parent) {
    final Node clonedNode = cloneNode(parent);

    for (final Node child : this.children) {
      clonedNode.children.add(child.cloneTree(clonedNode));
    }

    return clonedNode;
  }

  public final Pair<Node, Node> cloneTree(final Node parent, final Node subNode) {
    final Node clonedNode = cloneNode(parent);
    Node subNodeClone = null;

    for (final Node child : this.children) {
      final Pair<Node, Node> result = child.cloneTree(clonedNode, subNode);

      clonedNode.children.add(result.getFirst());

      if (result.getSecond() != null) {
        assert (subNodeClone == null);
        subNodeClone = result.getSecond();
      }
    }

    if (this == subNode) {
      assert (subNodeClone == null);
      subNodeClone = clonedNode;
    }

    return new Pair<Node, Node>(clonedNode, subNodeClone);
  }

  public final void findInvalidNodes() {
    if (!allGuardsSatisfied(false)) {
      if (isResolved()) {
        System.err.println(printTree());
      } else {
        System.err.println(this.getNodeName());
      }
    } else {
      for (final Node child : this.children) {
        child.findInvalidNodes();
      }
    }
  }


  // --- to implement in the sub-classes ---


  public abstract Node cloneNode(final Node parent);
  
  public abstract Class getNodeClass();

  public abstract List<Production> getAvailableProductions();

  public abstract void clearAttributeValues(final boolean recursive);

  public abstract void clearNonInheritedAttributeValues();

  public abstract boolean allInheritedAttributesEvaluated();

  public abstract boolean someSynthesizedAttributesEvaluated();

  public abstract boolean allGuardsSatisfied(final boolean checkChildren);

  public abstract boolean noGuardsFailing(final boolean checkChildren);

  public abstract boolean allInheritedAttributesMatch(final Node otherNode);

}
