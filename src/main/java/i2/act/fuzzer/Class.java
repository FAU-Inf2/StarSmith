package i2.act.fuzzer;

import i2.act.gengraph.GeneratorGraphNode.ClassNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Class implements ClassNode {
  
  public final int id;
  public final String name;

  public int minHeight;
  public int minSize;

  public final boolean isUnit;
  public final boolean isList;

  private final List<Production> productionList; // ensures deterministic ordering
  private final Map<String, Production> productionNames;

  public final List<Production> generatingProductions;

  private final List<Attribute> attributes;
  private final List<Attribute> inheritedAttributes;
  private final List<Attribute> synthesizedAttributes;
  private final List<Attribute> guardAttributes;

  // only used for generator classes
  private final boolean isGeneratorClass;
  private final int generatorPrecedence;
  private final List<AttributeRule> generatorAttributeRules;

  // only used for literal classes
  private final String regularExpression;
  private final int literalCount;

  private final int maxHeight;
  private final int maxWidth;

  private final int maxAlternatives;

  public Class(final int id, final String name, final boolean isUnit, final boolean isList,
      final boolean isGeneratorClass, final int generatorPrecedence, final Attribute[] attributes,
      final String regularExpression, final int literalCount, final int maxHeight,
      final int maxWidth, final int maxAlternatives) {
    this.id = id;
    this.name = name;

    this.isUnit = isUnit;
    this.isList = isList;

    this.isGeneratorClass = isGeneratorClass;
    this.generatorPrecedence = generatorPrecedence;
    this.generatorAttributeRules = new ArrayList<AttributeRule>();

    this.productionList = new ArrayList<Production>();
    this.productionNames = new HashMap<String, Production>();

    this.generatingProductions = new ArrayList<Production>();

    this.attributes = Collections.unmodifiableList(java.util.Arrays.asList(attributes));
    this.inheritedAttributes =
        filterAttributes(this.attributes, Attribute.AttributeKind.INHERITED);
    this.synthesizedAttributes =
        filterAttributes(this.attributes, Attribute.AttributeKind.SYNTHESIZED);
    this.guardAttributes =
        filterAttributes(this.attributes, Attribute.AttributeKind.GUARD);

    this.regularExpression = regularExpression;
    this.literalCount = literalCount;

    this.maxHeight = maxHeight;
    this.maxWidth = maxWidth;

    this.maxAlternatives = maxAlternatives;
  }

  private static final List<Attribute> filterAttributes(final List<Attribute> attributes,
      final Attribute.AttributeKind kind) {
    final List<Attribute> filteredAttributes = new ArrayList<>();

    for (final Attribute attribute : attributes) {
      if (attribute.getKind() == kind) {
        filteredAttributes.add(attribute);
      }
    }

    return Collections.unmodifiableList(filteredAttributes);
  }

  @Override
  public final List<Production> predecessors() {
    return Collections.unmodifiableList(this.generatingProductions);
  }

  @Override
  public final List<Production> successors() {
    return Collections.unmodifiableList(this.productionList);
  }

  @Override
  public final boolean isLiteralNode() {
    return isLiteralClass();
  }

  @Override
  public final boolean isGeneratorNode() {
    return this.isGeneratorClass;
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

  public final int getMaxMinHeight() {
    int maxMinHeight = 0;
    for (final Production production : this.productionList) {
      final int productionMinHeight = production.getMinHeight();

      if (productionMinHeight > maxMinHeight) {
        maxMinHeight = productionMinHeight;
      }
    }

    return maxMinHeight;
  }

  @Override
  public final String getName(final boolean qualified) {
    return this.name;
  }

  @Override
  public final boolean isListClass() {
    return this.isList;
  }

  @Override
  public final boolean isGuardClass() {
    return !this.guardAttributes.isEmpty();
  }

  @Override
  public final boolean isUnitClass() {
    return this.isUnit;
  }

  public final boolean isLiteralClass() {
    return this.regularExpression != null;
  }

  public final boolean isGeneratorClass() {
    return this.isGeneratorClass;
  }

  public final String getRegularExpression() {
    return this.regularExpression;
  }

  public final int getLiteralCount() {
    return this.literalCount;
  }

  public final int getMaxHeight() {
    return this.maxHeight;
  }

  public final int getMaxWidth() {
    return this.maxWidth;
  }

  public final int getMaxAlternatives() {
    return this.maxAlternatives;
  }

  public final void addProduction(final Production production) {
    this.productionList.add(production); // ensures deterministic ordering
    this.productionNames.put(production.name, production);

    for (final Class childClass : production.childClasses()) {
      childClass.generatingProductions.add(production);
    }
  }

  public final List<Production> getProductions() {
    return Collections.unmodifiableList(this.productionList);
  }

  public final Production getProductionByName(final String productionName) {
    return this.productionNames.get(productionName);
  }

  public final Production getProductionById(final int id) {
    for (final Production production : this.productionList) {
      if (production.id == id) {
        return production;
      }
    }

    return null;
  }

  public final int getNumberOfProductions() {
    return this.productionList.size();
  }

  public final List<Attribute> getAttributes() {
    return this.attributes;
  }

  public final List<Attribute> getInheritedAttributes() {
    return this.inheritedAttributes;
  }

  public final List<Attribute> getSynthesizedAttributes() {
    return this.synthesizedAttributes;
  }

  public final List<Attribute> getGuardAttributes() {
    return this.guardAttributes;
  }

  public final boolean hasInheritedAttribute() {
    return !this.inheritedAttributes.isEmpty();
  }

  public final boolean hasSynthesizedAttribute() {
    return !this.synthesizedAttributes.isEmpty();
  }

  public final boolean hasGuardAttribute() {
    return !this.guardAttributes.isEmpty();
  }

  public final void addGeneratorAttributeRule(final AttributeRule generatorAttributeRule) {
    this.generatorAttributeRules.add(generatorAttributeRule);
  }

  public final List<AttributeRule> getGeneratorAttributeRules() {
    return Collections.unmodifiableList(this.generatorAttributeRules);
  }

  public final int getGeneratorPrecedence() {
    return this.generatorPrecedence;
  }

  @Override
  public final String toString() {
    return this.name;
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof Class)) {
      return false;
    }

    final Class otherClass = (Class) other;
    return this.id == otherClass.id;
  }

  @Override
  public final int hashCode() {
    return this.id;
  }

  public final Node createNode(final Node parent, final int allowedHeight,
      final int allowedWidth) {
    return createNode(parent, allowedHeight, allowedWidth, null);
  }


  // --- to implement in the sub-classes ---


  public abstract Node createNode(final Node parent, final int allowedHeight,
      final int allowedWidth, final Node expected);

}
