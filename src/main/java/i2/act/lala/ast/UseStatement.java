package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class UseStatement extends LaLaASTNode {

  private final Identifier namespace;

  public UseStatement(final SourceRange sourceRange, final Identifier namespace) {
    super(sourceRange);
    this.namespace = namespace;
  }

  public final Identifier getNamespace() {
    return this.namespace;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
