package i2.act.errors.specification.semantics.symbols;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.info.SourcePosition;

public final class SymbolNotDefinedException extends InvalidLanguageSpecificationException {

  private final String undefinedSymbol;

  private SymbolNotDefinedException(final String undefinedSymbol,
      final LanguageSpecificationError error) {
    super(error);
    this.undefinedSymbol = undefinedSymbol;
  }

  public static final SymbolNotDefinedException buildFor(final String undefinedSymbol,
      final SourcePosition sourcePosition) {
    final LanguageSpecificationError error = new LanguageSpecificationError(sourcePosition,
        String.format("symbol '%s' not defined", undefinedSymbol));
    
    return new SymbolNotDefinedException(undefinedSymbol, error);
  }

  public final String getUndefinedSymbol() {
    return this.undefinedSymbol;
  }

}
