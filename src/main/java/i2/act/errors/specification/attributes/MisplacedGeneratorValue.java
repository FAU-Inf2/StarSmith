package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.GeneratorValue;

public final class MisplacedGeneratorValue extends InvalidLanguageSpecificationException {

  private final GeneratorValue generatorValue;

  public MisplacedGeneratorValue(final GeneratorValue generatorValue) {
    super(new LanguageSpecificationError(generatorValue.getSourcePosition(),
        "usage of generator value not allowed here"));

    this.generatorValue = generatorValue;
  }

  public final GeneratorValue getGeneratorValue() {
    return this.generatorValue;
  }

}
