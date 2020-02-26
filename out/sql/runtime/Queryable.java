package runtime;

import java.util.ArrayList;
import java.util.TreeMap;

import i2.act.fuzzer.Node;

public abstract class Queryable {

  protected final String name;
  protected final TableColumns columns;
  protected final String schemaName;

  protected Queryable(String name, TableColumns columns, String schemaName) {
    this.name = name;
    this.columns = columns;
    this.schemaName = schemaName;
  }

  public static String getSchemaName(Queryable q) {
    return q.getSchemaName();
  }

  public static String getName(Queryable q) {
    return q.getName();
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getName() {
    return name;
  }

  public static TableColumns getAllTableColumns(final Queryable q,
      final boolean withIdentityColumns) {
    if (withIdentityColumns) {
      return q.columns;
    } else {
      java.util.List<TableColumn> columns = TableColumns.getAllTableColumnsRaw(q.columns);
      java.util.List<TableColumn> newcolumns = new ArrayList<>();
      for (TableColumn column : columns) {
        if (TableColumn.isUnique(column)) continue;
        newcolumns.add(column.clone());
      }
      return TableColumns.getNewFromColumnList(newcolumns);
    }
  }

  public static boolean hasIdentityColumn(final Queryable q) {
    java.util.List<TableColumn> columns = TableColumns.getAllTableColumnsRaw(q.columns);
    return columns.stream().anyMatch(tc -> TableColumn.isUnique(tc));
  }

  public static boolean hasInsertableColumn(final Queryable q) {
    java.util.List<TableColumn> columns = TableColumns.getAllTableColumnsRaw(q.columns);
    return columns.stream().anyMatch(tc -> !TableColumn.isUnique(tc));
  }

  public static TableColumns getRandomColumns(final Queryable q, final boolean withIdentityColumns,
      final Node node){
    java.util.List<TableColumn> columns = TableColumns.getAllTableColumnsRaw(q.columns);
    java.util.List<TableColumn> newcolumns = new ArrayList<>();

    java.util.Random rand = new java.util.Random(node.id);

    for (TableColumn column : columns) {
      if (!withIdentityColumns && TableColumn.isUnique(column)) continue;
      if (rand.nextBoolean()) {
        newcolumns.add(column.clone());
      }
    }

    if (newcolumns.isEmpty()) {
      for (TableColumn column : columns) {
        if (TableColumn.isUnique(column)) continue;

        newcolumns.add(column.clone());
        break;
      }
    }

    return TableColumns.getNewFromColumnList(newcolumns);
  }

}
