package runtime;

import java.util.SortedMap;
import java.util.TreeMap;

import java.util.ArrayList;
import java.util.Set;

import i2.act.fuzzer.Node;

public final class SchemaSet {

  public static final SchemaSet emptySet() {
    return new SchemaSet();
  }

  public static final SchemaSet add(final SchemaSet set, final Schema schema) {
    if (Schema.getSchemaName(schema) == null) return set;

    final SchemaSet newSet = set.clone();
    newSet.elements.put(Schema.getSchemaName(schema), schema);

    return newSet;
  }

  public static final SchemaSet remove(final SchemaSet set, final String schema_name) {
    if (!contains(set, schema_name)) return set;

    final SchemaSet newSet = set.clone();
    newSet.elements.remove(schema_name);

    return newSet;
  }

  public static final boolean contains(final SchemaSet set, final String schema_name) {
    return set.elements.containsKey(schema_name);
  }

  public static final boolean containsSchemaWithName(final SchemaSet set, final String name) {
    return contains(set, name);
  }

  public static final String getRandomSchemaName(final SchemaSet set, final Node node) {
    final java.util.Set<String> keys = set.elements.keySet();

    int num_elems = keys.size();

    if (num_elems <= 0) {
      throw new RuntimeException("Called randomSchemaName without elements");
    }

    int random_chosen_elem = Random.nextInt(node, num_elems);

    int i = 0;
    for (String key : keys) {
      if (i == random_chosen_elem) {
        return key;
      }
      i++;
    }

    throw new RuntimeException("Chose an random element which is not in range!");
  }

  public static final boolean containsTables(final SchemaSet set) {
    return !getSchemasWithTables(set).isEmpty();
  }

  public static final boolean containsViews(final SchemaSet set) {
    return !getSchemasWithViews(set).isEmpty();
  }

  public static final boolean containsQueryable(final SchemaSet set) {
    return containsTables(set) || containsViews(set);
  }

  public static final Table getRandomTable(final SchemaSet set, final Node node) {
    java.util.List<String> schemasWithTables = getSchemasWithTables(set);
    if(schemasWithTables.isEmpty()) throw new RuntimeException("No Table found!");

    int random_chosen_elem = Random.nextInt(node, schemasWithTables.size());

    Schema chosen_schema_with_tables = set.elements.get(schemasWithTables.get(random_chosen_elem));
    return Schema.getRandomTable( chosen_schema_with_tables, node);
  }

  public static final View getRandomView(final SchemaSet set, final Node node) {
    java.util.List<String> viewsWithTables = getSchemasWithViews(set);
    if(viewsWithTables.isEmpty()) throw new RuntimeException("No View found!");

    int random_chosen_elem = Random.nextInt(node, viewsWithTables.size());

    Schema chosen_schema_with_views = set.elements.get(viewsWithTables.get(random_chosen_elem));
    return Schema.getRandomView( chosen_schema_with_views, node);
  }

  public static final SchemaSet addTable(final SchemaSet set, final Table table) {
    Schema clone_schema = Schema.addTable(set.elements.get(table.getSchemaName()), table);

    SchemaSet clone = SchemaSet.add(set, clone_schema);

    return clone;
  }

  public static final SchemaSet addView(final SchemaSet set, final View view) {
    Schema clone_schema = Schema.addView(set.elements.get(View.getSchemaName(view)), view);

    SchemaSet clone = SchemaSet.add(set, clone_schema);

    return clone;
  }

  public static final java.util.List<String> getSchemasWithTables(final SchemaSet set) {
    java.util.List<String> schemasWithTables = new ArrayList<>();

    for (java.util.Map.Entry<String,Schema> entry : set.elements.entrySet()) {
      if (Schema.containsTables(entry.getValue())) {
        schemasWithTables.add(entry.getKey());
      }
    }
    return schemasWithTables;
  }

  public static final java.util.List<String> getSchemasWithViews(final SchemaSet set) {
    java.util.List<String> schemasWithViews = new ArrayList<>();

    for (java.util.Map.Entry<String,Schema> entry : set.elements.entrySet()) {
      if (Schema.containsViews(entry.getValue())) {
        schemasWithViews.add(entry.getKey());
      }
    }
    return schemasWithViews;
  }


  public static final Schema getSchemaByName(final SchemaSet set, final String name) {
    return set.elements.get(name);
  }

  public static boolean isEmpty(final SchemaSet set) {
    return set.elements.isEmpty();
  }

  // ===============================================================================================

  public final java.util.SortedMap<String, Schema> elements;

  private SchemaSet() {
    this.elements = new TreeMap<String, Schema>();
  }

  protected final SchemaSet clone() {
    final SchemaSet clone = new SchemaSet();
    clone.elements.putAll(this.elements);

    return clone;
  }

}
