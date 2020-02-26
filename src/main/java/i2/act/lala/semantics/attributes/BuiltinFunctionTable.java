package i2.act.lala.semantics.attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i2.act.lala.semantics.attributes.BuiltinFunction.BuiltinFunctionBinary;
import static i2.act.lala.semantics.attributes.BuiltinFunction.BuiltinFunctionCall;
import static i2.act.lala.semantics.attributes.BuiltinFunction.BuiltinFunctionChainable;
import static i2.act.lala.semantics.attributes.BuiltinFunction.BuiltinFunctionUnary;

public final class BuiltinFunctionTable {

  private static final Map<String, BuiltinFunction> builtinFunctions = new HashMap<>();

  private static final void addFunction(final BuiltinFunction function) {
    builtinFunctions.put(function.getName(), function);
  }
  
  static {
    addFunction(new BuiltinFunctionChainable("and", "&&"));
    addFunction(new BuiltinFunctionChainable("or", "||"));
    addFunction(new BuiltinFunctionUnary("not", "!", ""));

    addFunction(new BuiltinFunctionUnary("isNil", "", " == null"));

    addFunction(new BuiltinFunction("if", 3, 3) {
    
      @Override
      public final String generateJavaCode(final List<String> codeOperands) {
        assert (codeOperands.size() == 3);

        final String codeCondition = codeOperands.get(0);
        final String codeThen = codeOperands.get(1);
        final String codeElse = codeOperands.get(2);

        return String.format("((%s) ? (%s) : (%s))", codeCondition, codeThen, codeElse);
      }

    });

    addFunction(new BuiltinFunctionUnary("int", "Integer.parseInt(", ")"));

    addFunction(new BuiltinFunctionChainable("+", "+"));
    addFunction(new BuiltinFunctionChainable("-", "-"));
    addFunction(new BuiltinFunctionChainable("*", "*"));
    addFunction(new BuiltinFunctionChainable("/", "/"));

    addFunction(new BuiltinFunctionBinary("==", "=="));
    addFunction(new BuiltinFunctionBinary("!=", "!="));
    addFunction(new BuiltinFunctionBinary("<", "<"));
    addFunction(new BuiltinFunctionBinary(">", ">"));
    addFunction(new BuiltinFunctionBinary("<=", "<="));
    addFunction(new BuiltinFunctionBinary(">=", ">="));

    addFunction(new BuiltinFunctionCall("equals", "java.util.Objects.equals", 2, 2));
    addFunction(new BuiltinFunctionCall("max", "Math.max", 2, 2));
    addFunction(new BuiltinFunctionCall("min", "Math.min", 2, 2));
    addFunction(new BuiltinFunctionUnary("len", "", ".length()"));
  }

  public static final boolean has(final String name) {
    return builtinFunctions.containsKey(name);
  }

  public static final BuiltinFunction get(final String name) {
    return builtinFunctions.get(name);
  }

}
