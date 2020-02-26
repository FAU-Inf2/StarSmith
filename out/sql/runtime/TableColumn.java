package runtime;

import java.util.ArrayList;
import java.util.TreeMap;

public class TableColumn {

  public static final TableColumn getNew(final String column_name, final Type col_type,
      final boolean is_unique) {
    return new TableColumn(column_name, col_type, is_unique);
  }

  public static final String getColumnName(final TableColumn tablecolumn) {
    return tablecolumn.column_name;
  }

  public static final Type getType(final TableColumn tablecolumn) {
    return tablecolumn.column_type;
  }

  public static final boolean isUnique(final TableColumn tablecolumn) {
    return tablecolumn.is_unique;
  }

  private String column_name;
  private Type column_type;
  private boolean is_unique;

  private TableColumn(final String name, final Type typ, final boolean is_unique) {
    this.column_name = name;
    this.column_type = typ;
    this.is_unique = is_unique;
  }

  protected final TableColumn clone() {
    final TableColumn clone = new TableColumn(this.column_name, this.column_type, this.is_unique);
    return clone;
  }
}
