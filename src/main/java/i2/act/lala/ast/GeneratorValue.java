package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class GeneratorValue extends AttributeExpression {

  private AttributeFunctionCall generatorCall;
  private AttributeTypeName typeName;

  public GeneratorValue(final SourceRange sourceRange) {
    super(sourceRange);
  }

  public final AttributeFunctionCall getGeneratorCall() {
    return this.generatorCall;
  }

  public final void setGeneratorCall(final AttributeFunctionCall generatorCall) {
    this.generatorCall = generatorCall;
  }

  public final AttributeTypeName getTypeName() {
    return this.typeName;
  }

  public final void setTypeName(final AttributeTypeName typeName) {
    this.typeName = typeName;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }
  
}
