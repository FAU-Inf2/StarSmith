package i2.act.lala.ast;

import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AnnotationSymbol;
import i2.act.lala.semantics.symbols.ClassSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ClassDeclaration extends Declaration
    implements AnnotatableDeclaration, ClassNode {

  private final List<Annotation> annotations;
  private final List<AttributeDeclaration> attributeDeclarations;

  private int minHeight;
  private int minSize;

  private final List<ProductionDeclaration> generatingProductions;

  private boolean isAutoUnit;
  
  public ClassDeclaration(final SourceRange sourceRange, final Identifier className) {
    super(sourceRange, className);

    this.annotations = new ArrayList<Annotation>();
    this.attributeDeclarations = new ArrayList<AttributeDeclaration>();

    this.generatingProductions = new ArrayList<ProductionDeclaration>();

    this.isAutoUnit = false;
  }

  @Override
  public final ClassSymbol getSymbol() {
    return (ClassSymbol) super.getSymbol();
  }

  public final Identifier getClassName() {
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

  public final void addAttributeDeclaration(final AttributeDeclaration attributeDeclaration) {
    this.attributeDeclarations.add(attributeDeclaration);
  }
  
  public final List<AttributeDeclaration> getAttributeDeclarations() {
    return Collections.unmodifiableList(this.attributeDeclarations);
  }

  @Override
  public final boolean isGuardClass() {
    for (final AttributeDeclaration attributeDeclaration : this.attributeDeclarations) {
      if (attributeDeclaration.getModifier().isGuardAttribute()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public final boolean isUnitClass() {
    return this.isAutoUnit || hasAnnotation(AnnotationSymbol.ANNOTATION_UNIT);
  }

  public final void setAutoUnit(final boolean isAutoUnit) {
    this.isAutoUnit = isAutoUnit;
  }

  public final void addGeneratingProduction(final ProductionDeclaration generatingProduction) {
    this.generatingProductions.add(generatingProduction);
  }

  @Override
  public final List<ProductionDeclaration> getGeneratingProductions() {
    return Collections.unmodifiableList(this.generatingProductions);
  }

  @Override
  public final List<ProductionDeclaration> predecessors() {
    return getGeneratingProductions();
  }

  @Override
  public final List<ProductionDeclaration> successors() {
    if (isLiteralNode()) {
      return Collections.emptyList();
    }

    final ClassSymbol symbol = getSymbol();
    assert (symbol != null) : "symbol has not been set";

    return symbol.getProductions().gatherSymbols().stream()
        .map(ps -> ps.getDeclaration())
        .collect(Collectors.toList());
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
  public final String getName(final boolean qualified) {
    return super.getName();
  }

}
