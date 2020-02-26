package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;

public interface StringElement {

  public <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter);

}
