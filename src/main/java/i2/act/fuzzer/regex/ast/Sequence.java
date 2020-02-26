package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sequence extends ASTNode {

  private final List<Atom> atoms;

  public Sequence(final List<Atom> atoms) {
    this.atoms = new ArrayList<Atom>(atoms);
  }

  public final List<Atom> getAtoms() {
    return Collections.unmodifiableList(this.atoms);
  }

  @Override
  public final boolean hasAlternatives() {
    for (final Atom atom : this.atoms) {
      if (atom.hasAlternatives()) {
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
