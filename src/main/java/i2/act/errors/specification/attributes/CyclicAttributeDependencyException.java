package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.ProductionDeclaration;

import java.util.Collection;
import java.util.stream.Collectors;

public final class CyclicAttributeDependencyException
    extends InvalidLanguageSpecificationException {

  private final Collection<ProductionDeclaration> cyclicProductions;

  public CyclicAttributeDependencyException(
      final Collection<ProductionDeclaration> cyclicProductions) {
    super(new LanguageSpecificationError(
        cyclicProductions.iterator().next().getSourcePosition(),
        String.format("attribute dependency cycle detected in the following productions: %s",
            cyclicProductions
                .stream()
                .map(ProductionDeclaration::getName)
                .collect(Collectors.joining(", ")))));

    this.cyclicProductions = cyclicProductions;
  }

  public final Collection<ProductionDeclaration> getCyclicProductions() {
    return this.cyclicProductions;
  }

}
