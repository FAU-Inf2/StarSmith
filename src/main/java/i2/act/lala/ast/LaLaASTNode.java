package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.info.SourceRange;

public abstract class LaLaASTNode {

  protected final SourceRange sourceRange;

  public LaLaASTNode(final SourceRange sourceRange) {
    this.sourceRange = sourceRange;
  }

  public final SourceRange getSourceRange() {
    return this.sourceRange;
  }

  public final SourcePosition getSourcePosition() {
    return this.sourceRange.getBegin();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  public abstract <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter);

}
