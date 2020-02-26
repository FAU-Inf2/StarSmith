package runtime;

import java.util.List;
import java.util.ArrayList;

public class QueryColumn {

  public static QueryColumn getNew(final String name, final Type type, final boolean is_unique) {
    return new QueryColumn(name, type, is_unique);
  }

  public static QueryColumn getNew(final String name, final Type type, final boolean is_unique,
      final String q_name) {
    QueryColumn col = new QueryColumn(name, type, is_unique);
    col.q_names.add(q_name);
    return col;
  }

  public static QueryColumn addName(QueryColumn col, String newname) {
    QueryColumn c = col.clone();
    c.q_names.add(newname);
    return c;
  }

  public static QueryColumn addNames(QueryColumn col, List<String> newnames) {
    return col.addNames(newnames);
  }

  public QueryColumn addNames(List<String> newnames) {
    QueryColumn c = clone();
    c.q_names.addAll(newnames);
    return c;
  }

  public List<String> getSchemaNames() {
    return q_names;
  }

  public static QueryColumn setName(QueryColumn col, String newname) {
    return col.setName(newname);
  }

  public QueryColumn setName(String newname) {
    QueryColumn c = this.clone(false);
    c.q_names.add(newname);
    return c;
  }

  public static String getName(QueryColumn qc) {
    return qc.getName();
  }

  public String getName() {
    return name;
  }

  public static String getFullName(QueryColumn qc) {
    return qc.getFullName();
  }

  public String getFullName() {
    if (q_names.isEmpty()) {
      //currently unqualified!
      return name;
    } else {
      //currently take the first one
      //but we still have the others, just in case!
      return String.format("%s.%s", q_names.get(0), name);
    }
  }

  public static Type getType(QueryColumn qc) {
    return qc.getType();
  }

  public Type getType() {
    return type;
  }

  public QueryColumn setUnique(boolean newUnique) {
    if (newUnique == this.isUnique) return this;

    QueryColumn nqc = clone();
    nqc.isUnique = newUnique;
    return nqc;
  }

  public static boolean isUnique(QueryColumn qc) {
    return qc.isUnique();
  }

  public boolean isUnique() {
    return isUnique;
  }

  private String name;
  private Type type;
  private List<String> q_names;
  private boolean isUnique;

  private QueryColumn(final String name, final Type type, final boolean isUnique) {
    this.name = name;
    this.type = type;
    this.isUnique = isUnique;
    this.q_names = new ArrayList<>();
  }

  public QueryColumn clone() {
    return clone(true);
  }

  public QueryColumn clone(boolean withNames) {
    QueryColumn c = new QueryColumn(name, type, isUnique);

    if (withNames) {
      c.q_names = new ArrayList<>(q_names);
    }
      
    return c;
  }

}
