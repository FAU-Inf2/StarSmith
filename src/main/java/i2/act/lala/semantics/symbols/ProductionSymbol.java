package i2.act.lala.semantics.symbols;

import i2.act.errors.RPGException;
import i2.act.lala.ast.ProductionDeclaration;

import java.util.List;

public final class ProductionSymbol extends Symbol<ProductionDeclaration> {

  private final ClassSymbol ownClassSymbol;

  private List<Integer> childVisitationOrder;

  public ProductionSymbol(final String name, final ProductionDeclaration productionDeclaration,
      final ClassSymbol ownClassSymbol) {
    super(name, productionDeclaration);

    this.ownClassSymbol = ownClassSymbol;
  }

  public final ClassSymbol getOwnClassSymbol() {
    return this.ownClassSymbol;
  }

  public final void setChildVisitationOrder(final List<Integer> childVisitationOrder) {
    if (childVisitationOrder.size() != this.declaration.getNumberOfChildDeclarations()) {
      throw new RPGException("size of visitation order does not match number of children");
    }

    this.childVisitationOrder = childVisitationOrder;
  }

  public final List<Integer> getChildVisitationOrder() {
    return this.childVisitationOrder;
  }

}
