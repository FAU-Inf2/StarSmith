package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scope<S extends Symbol<?>> {

  private final Map<String, S> symbols;

  public Scope() {
    this.symbols = new HashMap<String, S>();
  }

  public final S putSymbol(final S symbol) {
    final String name = symbol.getName();

    if (this.symbols.containsKey(name)) {
      final S originalSymbol = this.symbols.get(name);
      return originalSymbol;
    }

    this.symbols.put(name, symbol);
    return null;
  }

  public final S lookupSymbol(final Identifier identifier) {
    return lookupSymbol(identifier.getName());
  }

  public final S lookupSymbol(final String name) {
    return this.symbols.get(name);
  }

  public final void gatherSymbols(final List<S> symbols) {
    for (final S symbol : this.symbols.values()) {
      symbols.add(symbol);
    }
  }
  
}
