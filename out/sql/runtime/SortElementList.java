package runtime;

import java.util.stream.Collectors;

public class SortElementList extends ROL<SortElement> {

  public static SortElementList getEmpty() {
    return new SortElementList();
  }

  public SortElementList getNew() {
    return getEmpty();
  }

  public static SortElementList add_checked(ROL<SortElement> sel, SortElement se) {
    if(!SortElement.isRealSortElement(se)){
      return (SortElementList) sel;
    }
    return (SortElementList) add(sel, se);
  }

  public static String print(SortElementList sel) {
    return String.format("[%s]",
        String.join(", ", sel.list.stream().map(s -> s.toString()).collect(Collectors.toList())));
  }

  public static boolean isUniqueOrdering(SortElementList sel) {
    return sel.list.stream().anyMatch(se -> se.isUnique());
  }

  public boolean equalsAccordingToList(SortElement s1, SortElement s2) {
    assert false;
    return true;
  }

}
