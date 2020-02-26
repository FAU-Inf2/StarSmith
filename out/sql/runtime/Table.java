package runtime;

import java.util.ArrayList;
import java.util.TreeMap;

import i2.act.fuzzer.Node;

public final class Table extends Queryable {

  public static final Table getNew(final String tablename, final TableColumns columns,
      final String schemaname) {
    return new Table(tablename, columns, schemaname);
  }

  public static String getTableName(Table table) {
    return table.getName();
  }

  private Table(String tablename, TableColumns tablecolumns, String schemaname) {
    super(tablename, tablecolumns, schemaname);
  }

  protected final Table clone() {
    final Table clone = new Table(this.name, this.columns.clone(), this.schemaName);

    return clone;
  }

}
