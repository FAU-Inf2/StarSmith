package i2.act.lala.ast;

import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AnnotationSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;
import i2.act.lala.semantics.symbols.ProductionSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ProductionDeclaration extends Declaration
    implements AnnotatableDeclaration, ProductionNode {

  private final List<Annotation> annotations;
  private final List<LocalAttributeDefinition> localAttributeDefinitions;
  private final List<AttributeEvaluationRule> attributeEvaluationRules;

  private ChildSymbol thisSymbol;

  protected int minHeight;
  protected int minSize;

  public ProductionDeclaration(final SourceRange sourceRange, final Identifier productionName) {
    super(sourceRange, productionName);
    this.annotations = new ArrayList<Annotation>();
    this.localAttributeDefinitions = new ArrayList<>();
    this.attributeEvaluationRules = new ArrayList<>();
  }

  // helper method to simplify visitors
  public abstract LaLaASTNode getContent();

  @Override
  public final ProductionSymbol getSymbol() {
    return (ProductionSymbol) super.getSymbol();
  }

  public final Identifier getProductionName() {
    return getIdentifier();
  }

  public final void addAnnotation(final Annotation annotation) {
    this.annotations.add(annotation);
  }

  public final List<Annotation> getAnnotations() {
    return Collections.unmodifiableList(this.annotations);
  }

  @Override
  public final Annotation findAnnotation(final AnnotationSymbol annotationSymbol) {
    for (final Annotation annotation : this.annotations) {
      final AnnotationSymbol currentAnnotationSymbol = annotation.getSymbol();
      if (currentAnnotationSymbol == annotationSymbol) {
        return annotation;
      }
    }

    return null;
  }

  @Override
  public final boolean hasAnnotation(final AnnotationSymbol annotationSymbol) {
    return findAnnotation(annotationSymbol) != null;
  }

  public final void addLocalAttributeDefinition(
      final LocalAttributeDefinition localAttributeDefinition) {
    this.localAttributeDefinitions.add(localAttributeDefinition);
  }

  public final void addAttributeEvaluationRule(
      final AttributeEvaluationRule attributeEvaluationRule) {
    this.attributeEvaluationRules.add(attributeEvaluationRule);
  }

  public final List<LocalAttributeDefinition> getLocalAttributeDefinitions() {
    return Collections.unmodifiableList(this.localAttributeDefinitions);
  }

  public final List<AttributeEvaluationRule> getAttributeEvaluationRules() {
    return Collections.unmodifiableList(this.attributeEvaluationRules);
  }

  public final ChildSymbol getThisSymbol() {
    return this.thisSymbol;
  }

  public final void setThisSymbol(final ChildSymbol thisSymbol) {
    this.thisSymbol = thisSymbol;
  }

  @Override
  public final List<ClassDeclaration> predecessors() {
    return Arrays.asList(ownClassNode());
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
  public final boolean isLiteralNode() {
    return false;
  }

  @Override
  public final String getName(final boolean qualified) {
    if (qualified) {
      return String.format("%s::%s", ownClassNode().getName(true), super.getName());
    } else {
      return super.getName();
    }
  }

  @Override
  public final ClassDeclaration ownClassNode() {
    final ProductionSymbol symbol = getSymbol();
    assert (symbol != null) : "symbol has not been set";

    return symbol.getOwnClassSymbol().getDeclaration();
  }

  public abstract boolean isLeafProduction();

  public abstract List<ChildDeclaration> getChildDeclarations();

  public abstract int getNumberOfChildDeclarations();

}
