package runtime;

public class SortElement {

  public static SortElement getNew(int index, boolean asc, boolean nullfront, boolean unique) {
    return new SortElement(index, asc, nullfront, unique);
  }

  public String toString() {
    return String.format("[%d, %b, %b, %b]", index, asc, nullfront, unique);
  }

  public static boolean isRealSortElement(SortElement se) {
    return se.index != -1;
  }

  public boolean isUnique() {
    return unique;
  }

  private int index;
  private boolean asc;
  private boolean nullfront;
  private boolean unique;

  private SortElement(int index, boolean asc, boolean nullfront, boolean unique) {
    this.index = index;
    this.asc = asc;
    this.nullfront = nullfront;
    this.unique = unique;
  }

}
