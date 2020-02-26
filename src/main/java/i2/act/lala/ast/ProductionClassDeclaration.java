package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AnnotationSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProductionClassDeclaration extends ClassDeclaration {

  private final List<ProductionDeclaration> productionDeclarations;

  private boolean isGeneratorClass = false;

  public ProductionClassDeclaration(final SourceRange sourceRange, final Identifier className) {
    super(sourceRange, className);
    this.productionDeclarations = new ArrayList<ProductionDeclaration>();
  }

  public final void addProductionDeclaration(final ProductionDeclaration productionDeclaration) {
    this.productionDeclarations.add(productionDeclaration);
  }
  
  public final List<ProductionDeclaration> getProductionDeclarations() {
    return Collections.unmodifiableList(this.productionDeclarations);
  }

  public final boolean isGeneratorClass() {
    return this.isGeneratorClass;
  }

  public final void isGeneratorClass(final boolean isGeneratorClass) {
    this.isGeneratorClass = isGeneratorClass;
  }

  @Override
  public final boolean isLiteralNode() {
    return false;
  }

  @Override
  public final boolean isGeneratorNode() {
    return this.isGeneratorClass;
  }

  @Override
  public final boolean isListClass() {
    return hasAnnotation(AnnotationSymbol.ANNOTATION_LIST);
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
