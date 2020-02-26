package i2.act.errors.specification.semantics.symbols;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.Declaration;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.semantics.symbols.Symbol;

public final class SymbolAlreadyDefinedException extends InvalidLanguageSpecificationException {

  private final Symbol<?> originalSymbol;
  private final Symbol<?> redefinedSymbol;

  private SymbolAlreadyDefinedException(final Symbol<?> originalSymbol,
      final Symbol<?> redefinedSymbol, final LanguageSpecificationError error) {
    super(error);
    this.originalSymbol = originalSymbol;
    this.redefinedSymbol = redefinedSymbol;
  }

  private static final SourcePosition getSourcePosition(final Symbol<?> symbol) {
    final Declaration declaration = symbol.getDeclaration();

    if (declaration == null) {
      return SourcePosition.UNKNOWN;
    }

    return declaration.getSourcePosition();
  }

  public static final SymbolAlreadyDefinedException buildFor(final Symbol<?> originalSymbol,
      final Symbol<?> redefinedSymbol) {
    final SourcePosition originalPosition = getSourcePosition(originalSymbol);
    final SourcePosition redefinedPosition = getSourcePosition(redefinedSymbol);

    final LanguageSpecificationError error = new LanguageSpecificationError(redefinedPosition,
        String.format("symbol '%s' already defined at [%s]",
            originalSymbol.getName(), originalPosition.toString()));
    
    return new SymbolAlreadyDefinedException(originalSymbol, redefinedSymbol, error);
  }

  public final Symbol<?> getOriginalSymbol() {
    return this.originalSymbol;
  }

  public final Symbol<?> getRedefinedSymbol() {
    return this.redefinedSymbol;
  }

}
