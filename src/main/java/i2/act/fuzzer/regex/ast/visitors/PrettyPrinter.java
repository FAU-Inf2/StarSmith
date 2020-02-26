package i2.act.fuzzer.regex.ast.visitors;

import i2.act.fuzzer.regex.ast.*;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;

public final class PrettyPrinter implements RegularExpressionVisitor<BufferedWriter, Void> {

  public final Void visit(final Bounds bounds, final BufferedWriter writer) {
    FileUtil.write(String.format("{%d,%d}", bounds.getMinimum(), bounds.getMaximum()), writer);

    return null;
  }

  public final Void visit(final i2.act.fuzzer.regex.ast.Character character,
      final BufferedWriter writer) {
    FileUtil.write(character.getChar(), writer);

    return null;
  }

  public final Void visit(final CharacterRange characterRange, final BufferedWriter writer) {
    FileUtil.write(
        String.format("%s-%s",
            characterRange.getLowerCharacter().getChar(),
            characterRange.getUpperCharacter().getChar()),
        writer);

    return null;
  }

  public final Void visit(final Group group, final BufferedWriter writer) {
    FileUtil.write("[", writer);

    for (final Range range : group.getRanges()) {
      range.accept(this, writer);
    }

    FileUtil.write("]", writer);

    if (group.hasBounds()) {
      final Bounds bounds = group.getBounds();
      bounds.accept(this, writer);
    }

    return null;
  }

  public final Void visit(final RegularExpression regularExpression, final BufferedWriter writer) {
    boolean first = true;
    for (final Sequence alternative : regularExpression.getAlternatives()) {
      if (!first) {
        FileUtil.write("|", writer);
      }
      first = false;

      alternative.accept(this, writer);
    }

    return null;
  }

  public final Void visit(final SingleCharacter singleCharacter, final BufferedWriter writer) {
    final i2.act.fuzzer.regex.ast.Character character = singleCharacter.getCharacter();
    character.accept(this, writer);

    return null;
  }

  public final Void visit(final Sequence sequence, final BufferedWriter writer) {
    for (final Atom atom : sequence.getAtoms()) {
      atom.accept(this, writer);
    }

    return null;
  }

  public final Void visit(final Subexpression subexpression, final BufferedWriter writer) {
    FileUtil.write("(", writer);

    final RegularExpression expression = subexpression.getExpression();
    expression.accept(this, writer);

    FileUtil.write(")", writer);

    return null;
  }

}
