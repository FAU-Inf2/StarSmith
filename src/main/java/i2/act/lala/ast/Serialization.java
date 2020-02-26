package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.ArrayList;
import java.util.List;

public final class Serialization extends LaLaASTNode {

  private final StringLiteral interpolatedString;

  public Serialization(final SourceRange sourceRange, final StringLiteral interpolatedString) {
    super(sourceRange);
    this.interpolatedString = interpolatedString;
  }

  public final StringLiteral getInterpolatedString() {
    return this.interpolatedString;
  }

  public final List<ChildDeclaration> getChildDeclarations() {
    final List<ChildDeclaration> childDeclarations = new ArrayList<ChildDeclaration>();

    final List<StringElement> stringElements = this.interpolatedString.getStringElements();
    for (final StringElement stringElement : stringElements) {
      if (stringElement instanceof ChildDeclaration) {
        childDeclarations.add((ChildDeclaration) stringElement);
      }
    }

    return childDeclarations;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
