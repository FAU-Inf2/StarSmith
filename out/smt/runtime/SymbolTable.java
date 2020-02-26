package runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class SymbolTable {

  public static final SymbolTable empty() {
    return new SymbolTable(true);
  }

  public static final SymbolTable put(final SymbolTable symbolTable, final Symbol symbol) {
    final SymbolTable clone = symbolTable.clone();

    clone.put(symbol);

    return clone;
  }

  // XXX performance could be improved
  public static final SymbolTable enterScopes(final SymbolTable symbolTable, final int count) {
    final SymbolTable clone = symbolTable.clone();

    for (int i = 0; i < count; ++i) {
      clone.scopes.add(new LinkedHashMap<String, Symbol>());
    }

    return clone;
  }

  // XXX performance could be improved
  public static final SymbolTable enterScope(final SymbolTable symbolTable) {
    return enterScopes(symbolTable, 1);
  }

  public static final SymbolTable leaveScopes(final SymbolTable symbolTable, final int count) {
    final SymbolTable clone = symbolTable.clone();

    for (int i = 0; i < count; ++i) {
      clone.scopes.removeLast();
    }

    return clone;
  }

  public static final SymbolTable leaveScope(final SymbolTable symbolTable) {
    return leaveScopes(symbolTable, 1);
  }

  public static final SymbolTable merge(final SymbolTable first, final SymbolTable second) {
    final SymbolTable merged = new SymbolTable(true);

    for (final Map<String, Symbol> scope : first.scopes) {
      for (final Symbol symbol : scope.values()) {
        merged.put(symbol);
      }
    }

    for (final Map<String, Symbol> scope : second.scopes) {
      for (final Symbol symbol : scope.values()) {
        merged.put(symbol);
      }
    }

    return merged;
  }

  public static final boolean mayDefine(final SymbolTable symbolTable, final String name) {
    return !symbolTable.scopes.getLast().containsKey(name);
  }

  public static final Symbol get(final SymbolTable symbolTable, final String name) {
    final Iterator<Map<String, Symbol>> scopeIterator = symbolTable.scopes.descendingIterator();
    while (scopeIterator.hasNext()) {
      final Map<String, Symbol> scope = scopeIterator.next();

      if (scope.containsKey(name)) {
        return scope.get(name);
      }
    }

    throw new RuntimeException("name not defined");
  }

  public static final boolean contains(final SymbolTable symbolTable, final String name) {
    final Iterator<Map<String, Symbol>> scopeIterator = symbolTable.scopes.descendingIterator();
    while (scopeIterator.hasNext()) {
      final Map<String, Symbol> scope = scopeIterator.next();

      if (scope.containsKey(name)) {
        return true;
      }
    }

    return false;
  }

  private static final LinkedHashMap<String, Symbol> flatten(final SymbolTable symbolTable) {
    final LinkedHashMap<String, Symbol> flattened = new LinkedHashMap<>();

    for (final Map<String, Symbol> scope : symbolTable.scopes) {
      for (final Symbol symbol : scope.values()) {
        flattened.put(symbol.name, symbol);
      }
    }

    return flattened;
  }

  public static final List<Symbol> visibleSymbols(final SymbolTable symbolTable,
      final Type expectedType) {
    final List<Symbol> visibleSymbols = new LinkedList<>();

    final LinkedHashMap<String, Symbol> flattened = flatten(symbolTable);
    for (final Symbol symbol : flattened.values()) {
      if (Type.assignable(symbol.type, expectedType)) {
        visibleSymbols.add(symbol);
      }
    }

    return visibleSymbols;
  }

  public static final List<Symbol> visibleFunctions(final SymbolTable symbolTable,
      final boolean nullary, final Type expectedReturnType) {
    final List<Symbol> visibleFunctions = new LinkedList<>();

    final LinkedHashMap<String, Symbol> flattened = flatten(symbolTable);
    for (final Symbol symbol : flattened.values()) {
      final Type type = symbol.type;

      if (((Type.isNullaryFunctionType(type) && nullary)
          || (Type.isNonNullaryFunctionType(type) && !nullary))
          && Type.assignable(Type.getReturnType(type), expectedReturnType)) {
        visibleFunctions.add(symbol);
      }
    }

    return visibleFunctions;
  }

  public static final boolean containsVisibleSymbolOfType(final SymbolTable symbolTable,
      final Type type) {
    final LinkedHashMap<String, Symbol> flattened = flatten(symbolTable);

    for (final Symbol symbol : flattened.values()) {
      final Type symbolType = Symbol.getType(symbol);
      if (Type.assignable(symbolType, type)) {
        return true;
      }

      if ((symbolType instanceof Type.FunctionType)
          && Type.assignable(((Type.FunctionType) symbolType).returnType, type)) {
        return true;
      }
    }

    return false;
  }

  // -----------------------------------------------------------------------------------------------

  public final LinkedList<Map<String, Symbol>> scopes;

  public SymbolTable(final boolean addScope) {
    this.scopes = new LinkedList<Map<String, Symbol>>();

    if (addScope) {
      this.scopes.add(new LinkedHashMap<String, Symbol>());
    }
  }

  public final SymbolTable clone() {
    final SymbolTable clone = new SymbolTable(false);

    for (final Map<String, Symbol> scope : this.scopes) {
      clone.scopes.add(new LinkedHashMap<String, Symbol>(scope));
    }

    return clone;
  }

  public final void put(final Symbol symbol) {
    this.scopes.getLast().put(symbol.name, symbol);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SymbolTable)) {
      return false;
    }

    final SymbolTable otherSymbolTable = (SymbolTable) other;
    return this.scopes.equals(otherSymbolTable.scopes);
  }

  @Override
  public final int hashCode() {
    throw new RuntimeException("hashCode() not supported");
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("[");

    boolean firstScope = true;
    for (final Map<String, Symbol> scope : this.scopes) {
      if (!firstScope) {
        builder.append(", ");
      }
      firstScope = false;

      builder.append("{");

      boolean firstEntry = true;
      for (final Map.Entry<String, Symbol> entry : scope.entrySet()) {
        if (!firstEntry) {
          builder.append(", ");
        }
        firstEntry = false;

        builder.append(String.format("%s: %s", entry.getKey(), entry.getValue()));
      }

      builder.append("}");
    }

    builder.append("]");

    return builder.toString();
  }

}
