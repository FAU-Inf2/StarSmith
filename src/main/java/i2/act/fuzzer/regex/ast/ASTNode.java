package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public abstract class ASTNode {

  public abstract boolean hasAlternatives();

  public abstract <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter);

}
