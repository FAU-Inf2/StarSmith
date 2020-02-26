package i2.act.lala.ast;

import i2.act.errors.RPGException;
import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.types.IntegerType;
import i2.act.lala.semantics.types.Type;

import java.math.BigInteger;

public final class Constant extends Expression {

  private final String value;

  public Constant(final SourceRange sourceRange, final String value) {
    super(sourceRange);
    this.value = value;
  }

  public final String getValue() {
    return this.value;
  }

  public final BigInteger asInt() {
    try {
      return new BigInteger(this.value);
    } catch (final Throwable throwable) {
      throw new RPGException(String.format("not a valid integer: '%s'", this.value));
    }
  }

  public final Type getInferredType() {
    // there are only integer constants until now
    return IntegerType.INSTANCE;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
