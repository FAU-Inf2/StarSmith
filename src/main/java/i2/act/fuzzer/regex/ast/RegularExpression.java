package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RegularExpression extends ASTNode {

  private final List<Sequence> alternatives;

  public RegularExpression(final List<Sequence> alternatives) {
    this.alternatives = new ArrayList<Sequence>(alternatives);
  }

  public final List<Sequence> getAlternatives() {
    return Collections.unmodifiableList(this.alternatives);
  }

  @Override
  public final boolean hasAlternatives() {
    if (this.alternatives.size() > 1) {
      return true;
    }

    for (final Sequence alternative : this.alternatives) {
      if (alternative.hasAlternatives()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
