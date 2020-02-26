package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AnnotationSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Annotation extends LaLaASTNode {

  private final Identifier identifier;
  private final List<Expression> arguments;

  public Annotation(final SourceRange sourceRange, final Identifier identifier) {
    super(sourceRange);
    this.identifier = identifier;
    this.arguments = new ArrayList<Expression>();
  }

  public final AnnotationSymbol getSymbol() {
    return (AnnotationSymbol) this.identifier.getSymbol();
  }

  public final Identifier getIdentifier() {
    return this.identifier;
  }

  public final void addArgument(final Expression argument) {
    this.arguments.add(argument);
  }

  public final int getNumberOfArguments() {
    return this.arguments.size();
  }

  public final Expression getArgument(final int index) {
    if (index < 0 || index >= this.arguments.size()) {
      return null;
    }

    return this.arguments.get(index);
  }

  public final List<Expression> getArguments() {
    return Collections.unmodifiableList(this.arguments);
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
