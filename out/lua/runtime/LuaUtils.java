package runtime;

import runtime.Type.TableType;

import i2.act.fuzzer.Node;
import i2.act.fuzzer.runtime.EmbeddedCode;

import java.util.Map;
import java.util.Random;

public final class LuaUtils {

  private static final Random rng = new Random();

  public static final EmbeddedCode print(final SymbolTable symbols) {
    final EmbeddedCode code = EmbeddedCode.create()
      .print("do").indent()
      .print("local ___pr = function(v)").indent()
      .print("if (type(v) == \"number\") then").indent()
      .print("if (v ~= v) then").indent()
      .print("io.write(\"nan\\n\");").unindent()
      .print("else").indent()
      .print("io.write(string.format(\"%.0f\\n\", math.floor(v)));").unindent()
      .print("end").unindent()
      .print("elseif (type(v) == \"boolean\") then").indent()
      .print("io.write(tostring(v==true), \"\\n\");").unindent()
      .print("elseif (type(v) == \"string\") then").indent()
      .print("io.write(v, \"\\n\");").unindent()
      .print("end").unindent()
      .println("end")
      .print("io.write('-----\\n');");

    for (final Symbol symbol : SymbolTable.visibleSymbols(symbols)) {
      print(symbol.name, symbol.type, code);
    }

    code
      .unindent()
      .print("end");

    return code;
  }

  public static final void print(final String name, final Type type, final EmbeddedCode code) {
    if (Type.isPrimitiveType(type)) {
      code.newline().print("___pr(" + name + "); --" + type.toString());
    } else if (Type.isTableType(type)) {
      // array elements
      {
        final Tuple arrayElements = ((TableType) type).arrayElements;
        final int size = Tuple.size(arrayElements);

        for (int index = 1; index <= size; ++index) {
          final Type elementType = (Type) arrayElements.elements.get(index - 1);
          print(String.format("%s[%d]", name, index), elementType, code);
        }
      }

      // named members
      {
        final Map<String, Type> members = ((TableType) type).members;

        for (final Map.Entry<String, Type> member : members.entrySet()) {
          final String memberName = member.getKey();
          final Type memberType = member.getValue();

          print(String.format("%s.%s", name, memberName), memberType, code);
        }
      }
    }
  }

  public static final EmbeddedCode printRandomInt(final Node node) {
    rng.setSeed(node.id);

    return EmbeddedCode.create().println(
        String.format("do io.write(%d, \"\\n\") end", rng.nextInt(99) + 1));
  }

}
