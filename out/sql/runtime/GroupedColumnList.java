package runtime;

public final class GroupedColumnList extends ROL<QueryColumn> {

  boolean isSingleGroup = false;

  public static GroupedColumnList setSingleGroup(GroupedColumnList old, boolean value) {
    if(value == old.isSingleGroup) return old;

    GroupedColumnList n = old.clone();
    n.isSingleGroup = value;
    return n;
  }

  public static boolean isGroupedAtAll(GroupedColumnList gcl) {
    return getSingleGroup(gcl) || !isEmpty(gcl);
  }

  public static boolean getSingleGroup(GroupedColumnList gcl) {
    return gcl.isSingleGroup;
  }

  public static GroupedColumnList getEmpty() {
    return new GroupedColumnList();
  }

  public GroupedColumnList getNew() {
    return getEmpty();
  }

  public boolean equalsAccordingToList(QueryColumn a, QueryColumn b) {
    return a.getFullName().equals(b.getFullName());
  }

  public GroupedColumnList clone() {
    GroupedColumnList n = (GroupedColumnList) super.clone();
    n.isSingleGroup = isSingleGroup;
    return n;
  }

}
