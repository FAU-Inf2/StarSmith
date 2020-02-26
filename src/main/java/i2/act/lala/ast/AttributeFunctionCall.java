package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AttributeFunctionCall extends AttributeExpression {

  private final AttributeFunction function;
  private final List<AttributeExpression> arguments;

  public AttributeFunctionCall(final SourceRange sourceRange,
       final AttributeFunction function) {
    super(sourceRange);
    this.function = function;
    this.arguments = new ArrayList<AttributeExpression>();
  }

  public final void addArgument(final AttributeExpression argument) {
    this.arguments.add(argument);
  }

  public final AttributeFunction getFunction() {
    return this.function;
  }

  public final List<AttributeExpression> getArguments() {
    return Collections.unmodifiableList(this.arguments);
  }

  public final int getNumberOfArguments() {
    return this.arguments.size();
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
