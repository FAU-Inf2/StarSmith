package i2.act.fuzzer.regex.ast.visitors;

import i2.act.fuzzer.regex.ast.*;

public interface RegularExpressionVisitor<P, R> {

  public R visit(final Bounds bounds, final P parameter);

  public R visit(final i2.act.fuzzer.regex.ast.Character character, final P parameter);

  public R visit(final CharacterRange characterRange, final P parameter);

  public R visit(final Group group, final P parameter);

  public R visit(final RegularExpression regularExpression, final P parameter);

  public R visit(final SingleCharacter singleCharacter, final P parameter);

  public R visit(final Sequence sequence, final P parameter);

  public R visit(final Subexpression subexpression, final P parameter);

}
