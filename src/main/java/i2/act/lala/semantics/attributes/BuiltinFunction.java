package i2.act.lala.semantics.attributes;

import java.util.List;

public abstract class BuiltinFunction {

  public static final int ARBITRARY_NUMBER = -1;

  private final String name;

  private final int minOperands;
  private final int maxOperands;

  public BuiltinFunction(final String name, final int minOperands, final int maxOperands) {
    this.name = name;
    this.minOperands = minOperands;
    this.maxOperands = maxOperands;
  }

  public final String getName() {
    return this.name;
  }

  public final int getMinOperands() {
    return this.minOperands;
  }

  public final int getMaxOperands() {
    return this.maxOperands;
  }

  public abstract String generateJavaCode(final List<String> codeOperands);


  // ---------------------------------------------------------------------------


  public static final class BuiltinFunctionNullary extends BuiltinFunction {

    private final String code;

    public BuiltinFunctionNullary(final String name, final String code) {
      super(name, 0, 0);
      this.code = code;
    }

    @Override
    public final String generateJavaCode(final List<String> codeOperands) {
      return this.code;
    }

  }

  public static final class BuiltinFunctionUnary extends BuiltinFunction {

    private final String prefix;
    private final String suffix;

    public BuiltinFunctionUnary(final String name, final String prefix, final String suffix) {
      super(name, 1, 1);
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override
    public final String generateJavaCode(final List<String> codeOperands) {
      assert (codeOperands.size() == 1);
      final String codeOperand = codeOperands.get(0);

      return String.format("(%s(%s)%s)", this.prefix, codeOperand, this.suffix);
    }

  }

  public static final class BuiltinFunctionBinary extends BuiltinFunction {

    private final String operator;

    public BuiltinFunctionBinary(final String name, final String operator) {
      super(name, 2, 2);
      this.operator = operator;
    }

    @Override
    public final String generateJavaCode(final List<String> codeOperands) {
      assert (codeOperands.size() == 2);

      final String codeLeftOperand = codeOperands.get(0);
      final String codeRightOperand = codeOperands.get(1);

      return String.format("((%s) %s (%s))", codeLeftOperand, this.operator, codeRightOperand);
    }

  }

  public static final class BuiltinFunctionChainable extends BuiltinFunction {

    private final String operator;

    public BuiltinFunctionChainable(final String name, final String operator) {
      super(name, 2, ARBITRARY_NUMBER);
      this.operator = operator;
    }

    @Override
    public final String generateJavaCode(final List<String> codeOperands) {
      final StringBuilder builder = new StringBuilder();

      builder.append("(");

      boolean first = true;
      for (final String codeOperand : codeOperands) {
        if (!first) {
          builder.append(" ");
          builder.append(this.operator);
          builder.append(" ");
        }
        first = false;

        builder.append("(");
        builder.append(codeOperand);
        builder.append(")");
      }

      builder.append(")");

      return builder.toString();
    }

  }

  public static final class BuiltinFunctionCall extends BuiltinFunction {

    private final String functionName;

    public BuiltinFunctionCall(final String name, final String functionName, final int minOperands,
        final int maxOperands) {
      super(name, minOperands, maxOperands);
      this.functionName = functionName;
    }

    @Override
    public final String generateJavaCode(final List<String> codeOperands) {
      final StringBuilder builder = new StringBuilder();

      builder.append("(");

      builder.append(this.functionName);
      builder.append("(");

      boolean first = true;
      for (final String codeOperand : codeOperands) {
        if (!first) {
          builder.append(", ");
        }
        first = false;

        builder.append("(");
        builder.append(codeOperand);
        builder.append(")");
      }

      builder.append("))");

      return builder.toString();
    }

  }

}
