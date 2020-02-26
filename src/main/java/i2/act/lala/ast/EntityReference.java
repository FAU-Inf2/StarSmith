package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.Symbol;

public final class EntityReference extends Expression {

  private final Identifier entityName;

  public EntityReference(final SourceRange sourceRange, final Identifier entityName) {
    super(sourceRange);
    this.entityName = entityName;
  }

  public final Identifier getEntityName() {
    return this.entityName;
  }

  public final Symbol<?> getSymbol() {
    return this.entityName.getSymbol();
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}

