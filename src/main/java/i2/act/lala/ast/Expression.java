package i2.act.lala.ast;

import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.types.Type;

public abstract class Expression extends LaLaASTNode {

  protected Type type;

  public Expression(final SourceRange sourceRange) {
    super(sourceRange);
  }

  public final void setType(final Type type) {
    this.type = type;
  }

  public final Type getType() {
    return this.type;
  }

}
