package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class TypeName extends LaLaASTNode {

  private final Identifier name;

  public TypeName(final SourceRange sourceRange, final Identifier name) {
    super(sourceRange);
    this.name = name;
  }

  public final Identifier getName() {
    return this.name;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
