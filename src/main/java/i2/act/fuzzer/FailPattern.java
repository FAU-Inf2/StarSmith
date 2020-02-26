package i2.act.fuzzer;

import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FailPattern {

  private static final class AttributeInstance {

    public final Attribute attribute;
    public final int targetNodeIndex;

    public AttributeInstance(final Attribute attribute, final int targetNodeIndex) {
      this.attribute = attribute;
      this.targetNodeIndex = targetNodeIndex;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof AttributeInstance)) {
        return false;
      }

      final AttributeInstance otherAttributeInstance = (AttributeInstance) other;

      return this.attribute == otherAttributeInstance.attribute
          && this.targetNodeIndex == otherAttributeInstance.targetNodeIndex;
    }

    @Override
    public final int hashCode() {
      int hash = 7;
      hash = 31 * hash + this.attribute.hashCode();
      hash = 31 * hash + this.targetNodeIndex;
      return hash;
    }

    @Override
    public final String toString() {
      return String.format("<%d:%s>", this.targetNodeIndex, this.attribute);
    }

  }

  public static final Production WILDCARD = null;

  private Production production;

  private final Set<AttributeInstance> handledAttributes;

  private FailPattern parent;
  private List<FailPattern> children;

  private FailPattern(final Production production, final FailPattern parent,
      final List<FailPattern> children) {
    replaceBy(production, children);
    this.parent = parent;
    this.handledAttributes = new HashSet<AttributeInstance>();
  }

  private final void replaceBy(final Production production, final List<FailPattern> children) {
    this.production = production;
    this.children = children;

    for (final FailPattern child : this.children) {
      child.parent = this;
    }
  }

  private static final FailPattern specificFailPattern(final Node node, final FailPattern parent) {
    if (!node.isResolved()) {
      return createWildcard();
    }

    final int numberOfChildren = node.getNumberOfChildren();

    final List<FailPattern> children = new ArrayList<>(numberOfChildren);
    final FailPattern failPattern = new FailPattern(node.getProduction(), parent, children);

    for (int childIndex = 0; childIndex < numberOfChildren; ++childIndex) {
      final FailPattern childFailPattern =
          specificFailPattern(node.getChild(childIndex), failPattern);
      children.add(childFailPattern);
    }

    return failPattern;
  }

  public static final FailPattern fromFailingRoot(final Node rootNode,
      final boolean useSpecificPatterns) {
    if (useSpecificPatterns) {
      return specificFailPattern(rootNode, null);
    }

    assert (rootNode.isResolved());

    final List<FailPattern> wildcardChildren =
        createWildcardChildren(rootNode.getNumberOfChildren());

    final FailPattern failPattern =
        new FailPattern(rootNode.getProduction(), null, wildcardChildren);

    final LinkedHashSet<AttributeInstance> attributes = new LinkedHashSet<>();
    for (final Attribute guardAttribute : rootNode.getNodeClass().getGuardAttributes()) {
      if (guardAttribute.hasValue(rootNode) && !((boolean) guardAttribute.getValue(rootNode))) {
        final AttributeInstance attributeInstance = new AttributeInstance(guardAttribute, -1);
        attributes.add(attributeInstance);
      }
    }
    constructFlesh(failPattern, rootNode, attributes);

    return failPattern;
  }

  public static final FailPattern fromFailingChild(final Node rootNode, final Node childNode,
      final List<FailPattern> failPatternsToResolve, final boolean useSpecificPatterns) {
    if (useSpecificPatterns) {
      return specificFailPattern(rootNode, null);
    }

    if (rootNode == childNode) {
      return createWildcard();
    }

    final Pair<FailPattern, FailPattern> spine = constructSpine(rootNode, childNode);

    final FailPattern failingChildNode = spine.getFirst();
    final FailPattern failPattern = spine.getSecond();

    // add nodes that the failing child node depends on
    {
      final int childIndex = childNode.getParent().getIndexOfChild(childNode);

      final LinkedHashSet<AttributeInstance> attributes = new LinkedHashSet<>();

      for (final Attribute inheritedAttribute : childNode.getNodeClass().getInheritedAttributes()) {
        final AttributeInstance attributeInstance =
            new AttributeInstance(inheritedAttribute, childIndex);
        attributes.add(attributeInstance);
      }

      constructFlesh(failingChildNode.parent, childNode.getParent(), attributes);
    }

    // augment the fail pattern with the current context
    if (!failPatternsToResolve.isEmpty()) {
      // 'failPatternsToResolve' contains fail patterns that can be matched against the failing
      // child node; however, for the next step we need fail patterns that can be matched against
      // the root node -> find roots
      final List<FailPattern> failPatterns =
          findRoots(failingChildNode, failPattern, failPatternsToResolve);

      augmentWithContext(failPattern, failingChildNode, failPatterns);
    }

    return failPattern;
  }

  public static final FailPattern fromHeightLimit(final Node rootNode,
      final Node childNode) {
    final Pair<FailPattern, FailPattern> spine = constructSpine(rootNode, childNode);

    final FailPattern failPattern = spine.getSecond();
    return failPattern;
  }

  public static final FailPattern forRootAlternative(final Node rootNode) {
    final List<FailPattern> children = createWildcardChildren(rootNode.getNumberOfChildren());

    final FailPattern failPattern = new FailPattern(rootNode.getProduction(), null, children);
    return failPattern;
  }

  private static final List<FailPattern> findRoots(final FailPattern failingChildNode,
      final FailPattern rootNode, final List<FailPattern> failPatternsToResolve) {
    // compute length of spine
    int spineLength = 0;
    {
      FailPattern currentNode = failingChildNode;
      while (currentNode != rootNode) {
        ++spineLength;

        currentNode = currentNode.parent;
        assert (currentNode != null);
      }
    }

    final List<FailPattern> failPatternRoots = new ArrayList<>();

    for (final FailPattern failPatternToResolve : failPatternsToResolve) {
      FailPattern failPatternRoot = failPatternToResolve;

      for (int step = 0; step < spineLength; ++step) {
        failPatternRoot = failPatternRoot.parent;
        assert (failPatternRoot != null);
      }

      failPatternRoots.add(failPatternRoot);
    }

    return failPatternRoots;
  }

  private static final Pair<FailPattern, FailPattern> constructSpine(final Node rootNode,
      final Node childNode) {
    final FailPattern failingChildNode =
        new FailPattern(WILDCARD, null, new ArrayList<FailPattern>());
    FailPattern failPattern = failingChildNode;

    Node currentNode = childNode;
    while (currentNode != rootNode) {
      final Node nextNode = currentNode.getParent();

      assert (nextNode != null);
      assert (nextNode.isResolved());

      final int childIndex = nextNode.getIndexOfChild(currentNode);
      assert (childIndex >= 0);

      final List<FailPattern> children = new ArrayList<>();
      {
        final int numberOfChildren = nextNode.getNumberOfChildren();
        for (int index = 0; index < numberOfChildren; ++index) {
          if (index == childIndex) {
            children.add(failPattern);
          } else {
            children.add(createWildcard());
          }
        }
      }

      final FailPattern nextFailPattern = new FailPattern(nextNode.getProduction(), null, children);
      failPattern.parent = nextFailPattern;

      failPattern = nextFailPattern;
      currentNode = nextNode;
    }

    return new Pair<>(failingChildNode, failPattern);
  }

  private static final void constructFlesh(final FailPattern failPatternNode,
      final Node treeNode, final LinkedHashSet<AttributeInstance> attributeInstances) {
    assert (treeNode.isResolved());

    final int numberOfChildren = treeNode.getNumberOfChildren();

    if (failPatternNode.production == WILDCARD) {
      final List<FailPattern> wildcardChildren = createWildcardChildren(numberOfChildren);
      failPatternNode.replaceBy(treeNode.getProduction(), wildcardChildren);
    }

    final LinkedHashSet<AttributeInstance> sourceAttributesParent = new LinkedHashSet<>();
    final List<LinkedHashSet<AttributeInstance>> sourceAttributesChildren = new ArrayList<>();
    {
      for (int i = 0; i < numberOfChildren; ++i) {
        sourceAttributesChildren.add(new LinkedHashSet<>());
      }
    }

    final int childNodeIndex;
    {
      if (treeNode.getParent() == null) {
        childNodeIndex = -1;
      } else {
        childNodeIndex = treeNode.getParent().getIndexOfChild(treeNode);
      }
    }

    final Production production = treeNode.getProduction();
    for (final AttributeInstance attributeInstance : attributeInstances) {
      if (failPatternNode.handledAttributes.contains(attributeInstance)) {
        continue;
      }

      final Attribute attribute = attributeInstance.attribute;
      final int targetNodeIndex = attributeInstance.targetNodeIndex;

      final AttributeRule attributeRule =
          production.getAttributeRuleFor(attribute, targetNodeIndex);
      assert (attributeRule != null);

      final int numberOfSourceAttributes = attributeRule.getNumberOfSourceAttributes();
      final Attribute[] sourceAttributes = attributeRule.getSourceAttributes();
      final int[] sourceNodeIndexes = attributeRule.getSourceNodeIndexes();

      for (int i = 0; i < numberOfSourceAttributes; ++i) {
        final Attribute sourceAttribute = sourceAttributes[i];
        final int sourceNodeIndex = sourceNodeIndexes[i];

        if (sourceNodeIndex == -1) {
          assert (sourceAttribute.isInherited());
          if (treeNode.getParent() != null) {
            assert (childNodeIndex != -1);

            final AttributeInstance sourceAttributeInstance =
                new AttributeInstance(sourceAttribute, childNodeIndex);

            sourceAttributesParent.add(sourceAttributeInstance);
          }
        } else {
          assert (sourceAttribute.isSynthesized());

          final AttributeInstance sourceAttributeInstance =
              new AttributeInstance(sourceAttribute, -1);

          sourceAttributesChildren.get(sourceNodeIndex).add(sourceAttributeInstance);
        }
      }
    }

    failPatternNode.handledAttributes.addAll(attributeInstances);

    if ((!sourceAttributesParent.isEmpty()) && (failPatternNode.parent != null)) {
      constructFlesh(failPatternNode.parent, treeNode.getParent(), sourceAttributesParent);
    }

    for (int childIndex = 0; childIndex < numberOfChildren; ++childIndex) {
      if (!sourceAttributesChildren.get(childIndex).isEmpty()) {
        constructFlesh(failPatternNode.children.get(childIndex),
            treeNode.getChildren().get(childIndex), sourceAttributesChildren.get(childIndex));
      }
    }
  }

  private static final void augmentWithContext(final FailPattern failPatternNode,
      final FailPattern failingChildNode, final List<FailPattern> failPatterns) {
    if (failPatternNode == failingChildNode) {
      return;
    }

    if (failPatternNode.isWildcard()) {
      final Production mostSpecific = getMostSpecific(failPatterns);

      if (mostSpecific == null) {
        // keep wildcard as is
      } else {
        // replace wildcard with most specific node
        final int numberOfChildren = mostSpecific.getNumberOfChildren();

        final List<FailPattern> wildcardChildren = createWildcardChildren(numberOfChildren);
        failPatternNode.replaceBy(mostSpecific, wildcardChildren);

      }
    }

    if (!failPatternNode.isWildcard()) {
      // visit children
      final int numberOfChildren = failPatternNode.children.size();
      for (int childIndex = 0; childIndex < numberOfChildren; ++childIndex) {
        final FailPattern childFailPatternNode = failPatternNode.getChild(childIndex);

        final List<FailPattern> childFailPatterns = new ArrayList<>();
        {
          for (final FailPattern failPattern : failPatterns) {
            assert (failPattern.production == null
                || failPattern.production == failPatternNode.production) :
                String.format("%s <--> %s", failPattern.production, failPatternNode.production);
            if (failPattern.production == failPatternNode.production) {
              childFailPatterns.add(failPattern.children.get(childIndex));
            }
          }
        }

        augmentWithContext(childFailPatternNode, failingChildNode, childFailPatterns);
      }
    }
  }

  private static final Production getMostSpecific(final List<FailPattern> failPatterns) {
    Production mostSpecific = null;

    for (final FailPattern failPattern : failPatterns) {
      if (failPattern.isWildcard()) {
        continue;
      }

      final Production failPatternProduction = failPattern.production;

      if (mostSpecific == null) {
        mostSpecific = failPatternProduction;
      } else {
        assert (mostSpecific == failPatternProduction);
      }
    }

    return mostSpecific;
  }

  private static final FailPattern createWildcard() {
    return createWildcard(null);
  }

  private static final FailPattern createWildcard(final FailPattern parent) {
    return new FailPattern(WILDCARD, parent, new ArrayList<FailPattern>());
  }

  private static final List<FailPattern> createWildcardChildren(final int number) {
    final List<FailPattern> wildcardChildren = new ArrayList<>();

    for (int i = 0; i < number; ++i) {
      final FailPattern wildcardChild = createWildcard();
      wildcardChildren.add(wildcardChild);
    }

    return wildcardChildren;
  }

  public final boolean isWildcard() {
    return this.production == WILDCARD;
  }

  // -----------------------------------------------------------------------------------------------

  public final Production getRootProduction() {
    return this.production;
  }

  public final boolean matches(final Node tree) {
    return matches(tree, -1);
  }

  public final boolean matches(final Node tree, final int placeholderIndex) {
    if (this.production == WILDCARD) {
      return true;
    }

    // XXX NOTE: this assumes that the reference of the generator value does not change!
    if (this.production.generatorValue != null
        && tree.getProduction() != null
        && this.production.generatorValue == tree.getProduction().generatorValue) {
      return true;
    }

    if (this.production != tree.getProduction()) {
      return false;
    }

    final int numberOfChildren = this.children.size();
    final List<Node> treeChildren = tree.getChildren();

    for (int index = 0; index < numberOfChildren; ++index) {
      if (index == placeholderIndex) {
        continue;
      }

      final FailPattern failPatternChild = this.children.get(index);
      final Node treeChild = treeChildren.get(index);
      if (!failPatternChild.matches(treeChild, -1)) {
        return false;
      }
    }

    return true;
  }

  public final FailPattern getChild(final int childIndex) {
    return this.children.get(childIndex);
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("(");

    if (this.production == WILDCARD) {
      builder.append("*");
    } else {
      builder.append(this.production.toString());
    }

    for (final FailPattern child : this.children) {
      builder.append(" ");
      builder.append(child.toString());
    }

    builder.append(")");

    return builder.toString();
  }

}
