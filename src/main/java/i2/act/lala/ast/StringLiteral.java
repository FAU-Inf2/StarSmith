package i2.act.lala.ast;

import i2.act.errors.RPGException;
import i2.act.lala.ast.visitors.BaseLaLaSpecificationVisitor;
import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StringLiteral extends Expression {

  private final List<StringElement> stringElements;
  
  public StringLiteral(final SourceRange sourceRange) {
    super(sourceRange);
    this.stringElements = new ArrayList<StringElement>();
  }

  public final void addStringElement(final StringElement stringElement) {
    this.stringElements.add(stringElement);
  }

  public final List<StringElement> getStringElements() {
    return Collections.unmodifiableList(this.stringElements);
  }

  public final StringElement getStringElement(final int index) {
    return this.stringElements.get(index);
  }

  public final int numberOfStringElements() {
    return this.stringElements.size();
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

  @Override
  public final String toString() {
    return toString(false);
  }

  public final String toString(final boolean escapeQuotes) {
    final StringBuilder builder = new StringBuilder();

    for (final StringElement stringElement : this.stringElements) {
      stringElement.accept(new BaseLaLaSpecificationVisitor<StringBuilder, Void>() {

            private int indentation = 0;

            private final void newline(final StringBuilder builder) {
              builder.append("\n");
              for (int i = 0; i < this.indentation; ++i) {
                builder.append("  ");
              }
            }

            @Override
            public final Void visit(final StringCharacters stringCharacters,
                final StringBuilder builder) {
              builder.append(stringCharacters.getCharacters());
              return null;
            }

            @Override
            public final Void visit(final EscapeSequence escapeSequence,
                final StringBuilder builder) {
              final EscapeSequence.EscapeToken escapeToken = escapeSequence.getEscapeToken();

              switch (escapeToken) {
                case ESCAPE_NEWLINE: {
                  newline(builder);
                  break;
                }
                case ESCAPE_INDENT: {
                  ++this.indentation;
                  newline(builder);
                  break;
                }
                case ESCAPE_UNINDENT: {
                  --this.indentation;
                  newline(builder);
                  break;
                }
                case ESCAPE_DOLLAR: {
                  builder.append("$");
                  break;
                }
                case ESCAPE_HASH: {
                  builder.append("#");
                  break;
                }
                case ESCAPE_QUOTE: {
                  if (escapeQuotes) {
                    builder.append("\\\"");
                  } else {
                    builder.append("\"");
                  }
                  break;
                }
                default: {
                  throw new RPGException("unknown escape token: " + escapeToken);
                }
              }

              return null;
            }

            @Override
            public final Void visit(final ChildDeclaration childDeclaration,
                final StringBuilder builder) {
              throw new RPGException("cannot convert child declaration to plain string");
            }

            @Override
            public final Void prolog(final LaLaASTNode node, final StringBuilder builder) {
              assert (false);
              return null;
            }

        }, builder);
    }

    return builder.toString();
  }

}
