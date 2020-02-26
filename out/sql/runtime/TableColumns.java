package runtime;

import java.util.ArrayList;
import java.util.TreeMap;

import i2.act.fuzzer.Node;

public final class TableColumns {

  public static final TableColumns getNew() {
    return new TableColumns();
  }

  public static final TableColumns getNewFromColumnList(
      final java.util.List<TableColumn> list) {
    return new TableColumns(list);
  }


  public static final TableColumns add(final TableColumns tcs, final TableColumn col) {
    final TableColumns newTCs = tcs.clone();
    newTCs.elements.add(col);

    return newTCs;
  }

  public static final boolean contains(final TableColumns tcs, final String unique_name) {
    for (TableColumn col : tcs.elements) {
      if (TableColumn.getColumnName(col).equals(unique_name)) {
        return true;
      }
    }

    return false;
  }

  public static final java.util.List<TableColumn> getAllTableColumnsRaw(final TableColumns tcs) {
    return tcs.elements;
  }

  public static final TableColumns getRandomTableColumns(final TableColumns tcs, final Node node) {
    java.util.List<TableColumn> allTableColumns = getAllTableColumnsRaw(tcs);
    java.util.List<TableColumn> chosenColumns = ListUtil.pickRandomSubset(allTableColumns, node);
    return new TableColumns(chosenColumns);
  }

  public static final boolean isEmpty(final TableColumns tcs) {
    return tcs.elements.isEmpty();
  }

  public static final boolean hasInsertable(final TableColumns tcs) {
    return tcs.elements.stream().anyMatch(tc -> !TableColumn.isUnique(tc));
  }

  public static final boolean hasUnique(final TableColumns tcs) {
    return tcs.elements.stream().anyMatch(tc -> TableColumn.isUnique(tc));
  }

  public static final String printCommaSeperated(final TableColumns tcs) {
    StringBuilder s = new StringBuilder();
    for (TableColumn tc : tcs.elements) {
      s.append(",");
      s.append(TableColumn.getColumnName(tc));
    }

    if (s.length() == 0) throw new RuntimeException("No tablecolumns found!");

    return new String(s.deleteCharAt(0));
  }

  public static final int size(final TableColumns tcs) {
    return tcs.elements.size();
  }

  public static final TableColumn get(final TableColumns tcs, int index) {
    return tcs.elements.get(index);
  }

  // ===============================================================================================

  public final java.util.ArrayList<TableColumn> elements;

  private TableColumns() {
    this.elements = new ArrayList<>();
  }

  private TableColumns(final java.util.List<TableColumn> list) {
    this.elements = new ArrayList<>(list);
  }

  protected final TableColumns clone() {
    final TableColumns clone = new TableColumns(this.elements);

    return clone;
  }

}
