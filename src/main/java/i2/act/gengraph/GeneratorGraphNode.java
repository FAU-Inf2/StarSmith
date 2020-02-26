package i2.act.gengraph;

import i2.act.fuzzer.Class;
import i2.act.fuzzer.Production;

import java.util.List;

public interface GeneratorGraphNode<T, O> {
  
  public static interface ProductionNode extends GeneratorGraphNode<Production, Class> {

    @SuppressWarnings("unchecked")
    default List<? extends ClassNode> getChildClassNodes() {
      return (List<ClassNode>) successors();
    }

    public ClassNode ownClassNode();

    default boolean isLeafProduction() {
      return this.successors().isEmpty();
    }
  
  }

  public static interface ClassNode extends GeneratorGraphNode<Class, Production> {

    @SuppressWarnings("unchecked")
    default List<? extends ProductionNode> getProductionNodes() {
      return (List<ProductionNode>) successors();
    }

    @SuppressWarnings("unchecked")
    default List<? extends ProductionNode> getGeneratingProductions() {
      return (List<ProductionNode>) predecessors();
    }

    @Override
    default void setIsRecursive(final boolean recursive) {
      assert (false) : "cannot set 'recursive' property for class nodes";
    }

    @Override
    default boolean isRecursive() {
      for (final ProductionNode productionNode : getProductionNodes()) {
        if (productionNode.isRecursive()) {
          return true;
        }
      }
      return false;
    }

    public boolean isListClass();

    public boolean isGuardClass();

    public boolean isUnitClass();
  
  }

  @SuppressWarnings("unchecked")
  default T get() {
    return (T) this;
  }

  public List<? extends GeneratorGraphNode<O, T>> predecessors();

  public List<? extends GeneratorGraphNode<O, T>> successors();

  public boolean isLiteralNode();

  public boolean isGeneratorNode();

  public void setMinHeight(final int minHeight);

  public int getMinHeight();

  public void setMinSize(final int minSize);

  public int getMinSize();

  public void setIsRecursive(final boolean recursive);

  public boolean isRecursive();

  public String getName(final boolean qualified);
  
  default String getName() {
    return getName(false);
  }

}
