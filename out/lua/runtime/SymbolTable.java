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

  public static final SymbolTable put(final SymbolTable symbolTable, final Symbol symbol,
      final boolean inGlobalScope) {
    final SymbolTable clone = symbolTable.clone();

    clone.put(symbol, inGlobalScope);

    return clone;
  }


  public static final SymbolTable putAll(final SymbolTable symbolTable, final Tuple symbolTuple) {
    final SymbolTable clone = symbolTable.clone();

    final int size = Tuple.size(symbolTuple);
    for (int idx = 0; idx < size; ++idx) {
      final Symbol symbol = (Symbol) symbolTuple.elements.get(idx);
      clone.put(symbol);
    }

    return clone;
  }

  public static final SymbolTable update(final SymbolTable symbolTable,
      final boolean localDeclaration, final Tuple symbolTuple, final Tuple typeTuple) {
    assert (Tuple.size(symbolTuple) == Tuple.size(typeTuple))
        : String.format("%s <=> %s", symbolTuple, typeTuple);

    final SymbolTable clone = symbolTable.clone();

    final Map<String, Symbol> globalScope = clone.scopes.get(0);

    final int size = Tuple.size(symbolTuple);
    for (int idx = 0; idx < size; ++idx) {
      final Symbol symbol = (Symbol) symbolTuple.elements.get(idx);
      if (symbol.name == null) {
        continue;
      }

      final Type type = (Type) typeTuple.elements.get(idx);

      final Symbol newSymbol = new Symbol(symbol.name, type);

      if (localDeclaration) {
        clone.put(newSymbol);
      } else {
        final Map<String, Symbol> containingScope = getContainingScope(clone, symbol.name);

        if (containingScope != null) {
          final Symbol oldSymbol = containingScope.get(symbol.name);
          assert (oldSymbol != null);
          assert (Type.assignable(type, oldSymbol.type))
              : String.format("cannot assign %s to %s", type, oldSymbol.type);
          // do nothing here, keep old type
        } else {
          // no declaration found -> has to be a global variable
          globalScope.put(symbol.name, newSymbol);
        }
      }
    }

    return clone;
  }

  // XXX performance could be improved
  public static final SymbolTable enterScope(final SymbolTable symbolTable) {
    final SymbolTable clone = symbolTable.clone();

    clone.scopes.add(new LinkedHashMap<String, Symbol>());

    return clone;
  }

  public static final SymbolTable leaveScope(final SymbolTable symbolTable) {
    final SymbolTable clone = symbolTable.clone();

    clone.scopes.removeLast();

    return clone;
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

  public static final SymbolTable common(final SymbolTable first, final SymbolTable second) {
    if (first == null) {
      return second;
    }

    if (second == null) {
      return first;
    }

    final SymbolTable common = new SymbolTable(false);

    assert (first.scopes.size() == second.scopes.size())
        : String.format("number of scopes does not match: %d vs. %d",
            first.scopes.size(), second.scopes.size());
    final int numberOfScopes = first.scopes.size();
    for (int scopeIndex = 0; scopeIndex < numberOfScopes; ++scopeIndex) {
      final Map<String, Symbol> firstScope = first.scopes.get(scopeIndex);
      final Map<String, Symbol> secondScope = second.scopes.get(scopeIndex);

      final LinkedHashMap<String, Symbol> commonScope = new LinkedHashMap<>();

      for (final Symbol firstSymbol : firstScope.values()) {
        final Symbol secondSymbol = secondScope.get(firstSymbol.name);

        if (secondSymbol != null) {
          assert (firstSymbol.name.equals(secondSymbol.name));

          if (firstSymbol.type.equals(secondSymbol.type)) {
              commonScope.put(firstSymbol.name, new Symbol(firstSymbol.name, firstSymbol.type));
          } else {
            if (Type.assignable(firstSymbol.type, secondSymbol.type)) {
              commonScope.put(secondSymbol.name, new Symbol(secondSymbol.name, secondSymbol.type));
            } else if (Type.assignable(secondSymbol.type, firstSymbol.type)) {
              commonScope.put(firstSymbol.name, new Symbol(firstSymbol.name, firstSymbol.type));
            } else {
              commonScope.put(firstSymbol.name, new Symbol(firstSymbol.name, Type.Nil()));
            }
          }
        }
      }

      common.scopes.add(commonScope);
    }

    return common;
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

  private static final Map<String, Symbol> getContainingScope(final SymbolTable symbolTable,
      final String name) {
    final Iterator<Map<String, Symbol>> scopeIterator = symbolTable.scopes.descendingIterator();
    while (scopeIterator.hasNext()) {
      final Map<String, Symbol> scope = scopeIterator.next();

      if (scope.containsKey(name)) {
        return scope;
      }
    }

    return null;
  }

  public static final boolean contains(final SymbolTable symbolTable, final String name) {
    return getContainingScope(symbolTable, name) != null;
  }

  public static final boolean isEmpty(final SymbolTable symbolTable) {
    for (final Map<String, Symbol> scope : symbolTable.scopes) {
      if (!scope.isEmpty()) {
        return false;
      }
    }

    return true;
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
      final Type expectedType, final boolean strictTableTypes) {
    final List<Symbol> visibleSymbols = new LinkedList<>();

    final LinkedHashMap<String, Symbol> flattened = flatten(symbolTable);
    for (final Symbol symbol : flattened.values()) {
      if (expectedType == null || Type.assignable(symbol.type, expectedType, strictTableTypes)) {
        visibleSymbols.add(symbol);
      }
    }

    return visibleSymbols;
  }

  public static final List<Symbol> visibleSymbols(final SymbolTable symbolTable) {
    return visibleSymbols(symbolTable, null, false);
  }

  public static final List<Symbol> visibleFunctions(final SymbolTable symbolTable,
      final Type expectedReturnType) {
    final List<Symbol> visibleSymbols = new LinkedList<>();

    final LinkedHashMap<String, Symbol> flattened = flatten(symbolTable);
    for (final Symbol symbol : flattened.values()) {
      if (!(symbol.type instanceof Type.FunctionType)) {
        continue;
      }

      final Type.FunctionType functionType = ((Type.FunctionType) symbol.type);
      final Type returnType = functionType.returnType;

      if (expectedReturnType == null || Type.assignable(returnType, expectedReturnType, false)) {
        visibleSymbols.add(symbol);
      }
    }

    return visibleSymbols;
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

  public final void put(final Symbol symbol, final boolean inGlobalScope) {
    if (inGlobalScope) {
      this.scopes.getFirst().put(symbol.name, symbol);
    } else {
      this.scopes.getLast().put(symbol.name, symbol);
    }
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
