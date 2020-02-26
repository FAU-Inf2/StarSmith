package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.Collections;
import java.util.List;

public final class GeneratorProductionDeclaration extends ProductionDeclaration {

  private final AttributeFunctionCall generatorCall;
  private final AttributeTypeName typeName;

  public GeneratorProductionDeclaration(final SourceRange sourceRange, 
      final Identifier productionName, final AttributeFunctionCall generatorCall,
      final AttributeTypeName typeName) {
    super(sourceRange, productionName);
    this.generatorCall = generatorCall;
    this.typeName = typeName;
  }

  @Override
  public final LaLaASTNode getContent() {
    return this.generatorCall;
  }

  public final AttributeFunctionCall getGeneratorCall() {
    return this.generatorCall;
  }

  public final AttributeTypeName getTypeName() {
    return this.typeName;
  }

  @Override
  public final boolean isLeafProduction() {
    return true; // a generator production does not have any children
  }

  @Override
  public final List<ChildDeclaration> getChildDeclarations() {
    return Collections.emptyList(); // a generator production does not have any children
  }

  @Override
  public final int getNumberOfChildDeclarations() {
    return 0; // a generator production does not have any children
  }

  @Override
  public final boolean isGeneratorNode() {
    return true;
  }

  @Override
  public final void setIsRecursive(final boolean recursive) {
    assert (false) : "cannot set 'recursive' property of generator production";
  }

  @Override
  public final boolean isRecursive() {
    return false; // generator productions are never recursive
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

  @Override
  public final List<ClassDeclaration> successors() {
    return Collections.emptyList(); // a generator production does not have any children
  }

}
