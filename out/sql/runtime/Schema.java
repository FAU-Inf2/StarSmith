package runtime;

import java.util.TreeMap;

import i2.act.fuzzer.Node;

public final class Schema {

  public static final Schema getEmptySchema() {
    return new Schema(null);
  }

  public static final Schema getNew(String schema_name) {
    return new Schema(schema_name);
  }

  public static final String getSchemaName(Schema schema) {
    return schema.schema_name;
  }

  public static final Schema addTable(final Schema schema, final Table table) {
    Schema clone = schema.clone();

    clone.tables.put(Table.getTableName(table), table);

    return clone;
  }

  public static final Schema addView(final Schema schema, final View view) {
    Schema clone = schema.clone();

    clone.views.put(View.getViewName(view), view);

    return clone;
  }

  public static final Schema removeTable(final Schema schema, final Table table) {
    Schema clone = schema.clone();

    clone.tables.remove(Table.getTableName(table));

    return clone;
  }

  public static final Schema removeView(final Schema schema, final View view) {
    Schema clone = schema.clone();

    clone.views.remove(View.getViewName(view));

    return clone;
  }

  public final static boolean containsTables(final Schema schema) {
    return !schema.tables.isEmpty();
  }

  public final static boolean containsViews(final Schema schema) {
    return !schema.views.isEmpty();
  }

  public final static boolean containsQueryable(final Schema schema) {
    return containsTables(schema) || containsViews(schema);
  }

  public static final boolean containsQueryableWithName(final Schema schema, final String name) {
    return schema.tables.containsKey(name) || schema.views.containsKey(name);
  }

  public final static boolean canBeDroppedRestricted(final Schema schema) {
    return !containsQueryable(schema);
  }

  public final static View getRandomView(final Schema schema, final Node node) {
    String view_name = Random.getRandomFromSet(schema.views.keySet(), node);
    return schema.views.get(view_name);
  }

  public final static Table getRandomTable(final Schema schema, final Node node) {
    String table_name = Random.getRandomFromSet(schema.tables.keySet(), node);
    return schema.tables.get(table_name);
  }

  // ===============================================================================================

  private final String schema_name;

  final java.util.TreeMap<String, Table> tables;
  final java.util.TreeMap<String, View> views;

  private Schema (final String name) {
    this.schema_name = name;
    this.tables = new TreeMap<>();
    this.views = new TreeMap<>();
  }

  protected final Schema clone() {
    final Schema clone = new Schema(schema_name);
    clone.tables.putAll(this.tables);
    clone.views.putAll(this.views);

    return clone;
  }

}
