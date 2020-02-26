package i2.act.lala.semantics.symbols;

import i2.act.errors.RPGException;
import i2.act.errors.specification.semantics.symbols.SymbolAlreadyDefinedException;
import i2.act.errors.specification.semantics.symbols.SymbolNotDefinedException;
import i2.act.lala.ast.Identifier;
import i2.act.lala.info.SourcePosition;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class SymbolTable<S extends Symbol<?>> {

  private final LinkedList<Scope<S>> scopes;

  public SymbolTable() {
    this.scopes = new LinkedList<Scope<S>>();
  }

  public final void enterScope() {
    final Scope<S> newScope = new Scope<S>();
    this.scopes.add(0, newScope);
  }

  public final void leaveScope() {
    if (this.scopes.isEmpty()) {
      throw new RPGException("no scope to leave");
    }

    this.scopes.removeFirst();
  }

  public final void putSymbol(final S symbol) throws SymbolAlreadyDefinedException {
    if (this.scopes.isEmpty()) {
      throw new RPGException("no scope to add symbol to");
    }

    final Scope<S> currentScope = this.scopes.peekFirst();
    final S originalSymbol = currentScope.putSymbol(symbol);

    if (originalSymbol != null) {
      throw SymbolAlreadyDefinedException.buildFor(originalSymbol, symbol);
    }
  }

  public final S lookupSymbol(final Identifier identifier) {
    return lookupSymbol(identifier.getName(), identifier.getSourcePosition());
  }

  public final S lookupSymbol(final String name, final SourcePosition sourcePosition)
      throws SymbolNotDefinedException {
    for (final Scope<S> scope : this.scopes) {
      final S symbol = scope.lookupSymbol(name);
      if (symbol != null) {
        return symbol;
      }
    }

    throw SymbolNotDefinedException.buildFor(name, sourcePosition);
  }

  public final List<S> gatherSymbols() {
    final List<S> symbols = new ArrayList<S>();

    for (final Scope<S> scope : this.scopes) {
      scope.gatherSymbols(symbols);
    }

    return symbols;
  }
  
}
