package runtime;

import java.util.ArrayList;
import java.util.TreeMap;

import i2.act.fuzzer.Node;

public final class View extends Queryable {

  public static final View getNew(final String viewname, final TableColumns columns,
      final String schemaname) {
    return new View(viewname, columns, schemaname);
  }

  public static final View getNew(final String viewname, final QueryColumnList columns,
      final String schemaname) {
    ArrayList<TableColumn> tcs = new ArrayList<>();

    for (QueryColumn qc : columns.getQueryColumns()) {
      tcs.add(TableColumn.getNew(qc.getName(), qc.getType(), qc.isUnique()));
    }

    TableColumns tabcols = TableColumns.getNewFromColumnList(tcs);
    return new View(viewname, tabcols, schemaname);
  }

  private View(String viewname, TableColumns columns, String schemaname) {
    super(viewname, columns, schemaname);
  }

  public static String getViewName(View view) {
    return view.getName();
  }

  protected final View clone() {
    final View clone = new View(this.name, this.columns.clone(), this.schemaName);

    return clone;
  }

}
