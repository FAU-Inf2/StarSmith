package runtime;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import java.util.stream.Collectors;

public class QueryResult {

  public static final QueryResult getEmpty() {
    return new QueryResult();
  }

  public static final QueryResult fromQueryable(final Queryable t, final boolean canBeUnique) {
    TableColumns cols = Table.getAllTableColumns(t, true);
    List<TableColumn> cols_raw = TableColumns.getAllTableColumnsRaw(cols);

    QueryResult ret = new QueryResult();

    for (TableColumn col : cols_raw) {
      ret.q_cols.add(QueryColumn.getNew(
        TableColumn.getColumnName(col),
        TableColumn.getType(col),
        TableColumn.isUnique(col) && canBeUnique
        ));
    }

    return ret;
  }

  public static final QueryResult fromQueryColumnList(QueryColumnList qcl, String name,
      final boolean canBeUnique) {
    QueryResult qr = new QueryResult();
    qr.names.add(name);

    for (QueryColumn qc : qcl.getQueryColumns()) {
      QueryColumn nqc = qc.setName(name);
      if (!canBeUnique) {
        nqc = nqc.setUnique(false);
      }
      qr.q_cols.add(nqc);
    }

    return qr;
  }

  public static final QueryResult mergeCrossJoin(QueryResult leftside, QueryResult rightside) {
    QueryResult qr = new QueryResult();

    //at first all columns of the left side
    for (QueryColumn lqc : getQueryColumns(leftside)) {
      qr.q_cols.add(lqc.clone());
    }

    //then all columns of the right side!
    for (QueryColumn rqc : getQueryColumns(rightside)) {
      qr.q_cols.add(rqc.clone());
    }

    qr.names.addAll(leftside.getNames());
    qr.names.addAll(rightside.getNames());

    return qr;
  }

  public static final QueryResult mergeQualifiedJoinJoincondition(QueryResult leftside,
      QueryResult rightside) {
    return mergeCrossJoin(leftside, rightside);
  }

  //important: use getName() here! schemanames are not relevant and not allowed here!!
  //keep order of leftside, so it can be used on a natural join!
  public static final List<String> getAvailableQualifiedJoinColumns(QueryResult leftside,
      QueryResult rightside) {
    HashMap<String, Integer> countLeftcols = new HashMap<>();
    HashMap<String, Integer> countRightcols = new HashMap<>();

    getQueryColumns(leftside).stream().forEach(
        qc -> countLeftcols.put(qc.getName(), countLeftcols.getOrDefault(qc.getName(), 0) + 1));
    getQueryColumns(rightside).stream().forEach(
        qc -> countRightcols.put(qc.getName(), countRightcols.getOrDefault(qc.getName(), 0) + 1));

    HashMap<String, QueryColumn> rightcols = new HashMap<>();
    for (QueryColumn rqc : getQueryColumns(rightside)) {
      rightcols.put(rqc.getName(), rqc);
    }

    List<String> usable = new ArrayList<>();
    for (QueryColumn leftcol : getQueryColumns(leftside)) {
      //exactly once in both
      QueryColumn rightcol = rightcols.get(leftcol.getName()); // can be null, but that's fine.
      if (countLeftcols.get(leftcol.getName()) == 1 && countRightcols.getOrDefault(leftcol.getName(), 0) == 1) {
        assert rightcol != null;
        //check if types are convertible!
        if (rightcol.getType().convertibleTo(leftcol.getType()) || leftcol.getType().convertibleTo(rightcol.getType())) {
          //can be used
          usable.add(leftcol.getName());
        }
      }
    }

    return usable;
  }

  // for each column on the right side,
  // there must exist at most one name with the same column
  // and the types must be implicit castable. (even the same type!)
  public static final boolean isNaturalJoinAllowed(QueryResult leftside, QueryResult rightside) {
    for (QueryColumn rightcol : getQueryColumns(rightside)) {
      String r_name = rightcol.getName();
      int found_matches = 0;
      for (QueryColumn leftcol : getQueryColumns(leftside)) {
        String l_name = leftcol.getName();
        if (l_name.equals(r_name)) {

          found_matches++;
          //types are not matching!
          if (!(rightcol.getType().convertibleTo(leftcol.getType())
              || leftcol.getType().convertibleTo(rightcol.getType()))) {
            return false;
          }
        }
      }

      // two column with same name found!
      if (found_matches > 1) {
        return false;
      }
    }

    return true;
  }

  public static final QueryResult mergeQualifiedJoinNamedColumnsJoin(QueryResult leftside,
      QueryResult rightside, List<String> columnnames) {
    QueryResult qr = new QueryResult();

    List<QueryColumn> querycolsleft = getQueryColumns(leftside);
    List<QueryColumn> querycolsright = getQueryColumns(rightside);

    //collect the used joincolumns.
    for (String colname : columnnames) {
      //there will be exactly one!
      QueryColumn lqc = querycolsleft.stream().filter(
          qc -> qc.getName().equals(colname)).findFirst().orElse(null);
      QueryColumn rqc = querycolsright.stream().filter(
          qc -> qc.getName().equals(colname)).findFirst().orElse(null);

      Type lt = lqc.getType();
      Type rt = rqc.getType();

      Type res_type;
      //at least one of them is convertible to the other! (see construction of columnnames
      if (Type.isImplicitConvertible(lt, rt)) {
        res_type = lt;
      } else {
        res_type = rt;
      }

      QueryColumn qc = QueryColumn.getNew(lqc.getName(), res_type, lqc.isUnique() && rqc.isUnique());
      qc = qc.addNames(leftside.getNames());
      qc = qc.addNames(rightside.getNames());
      qr.q_cols.add(qc);
    }

    //now collect all left
    for (QueryColumn lqc : getQueryColumns(leftside)) {
      if (!columnnames.contains(lqc.getName())) {
        qr.q_cols.add(lqc.clone());
      }
    }

    //now all right.
    for (QueryColumn rqc : getQueryColumns(rightside)) {
      if (!columnnames.contains(rqc.getName())) {
        qr.q_cols.add(rqc.clone());
      }
    }

    qr.names.addAll(leftside.getNames());
    qr.names.addAll(rightside.getNames());

    return qr;
  }

  public static final QueryResult mergeNaturalJoin(QueryResult leftside, QueryResult rightside) {
    List<String> allPossibleCols = getAvailableQualifiedJoinColumns(leftside, rightside);
    return mergeQualifiedJoinNamedColumnsJoin(leftside, rightside, allPossibleCols);
  }

  public static QueryResult setName(final QueryResult old_qr, final String newName) {
    QueryResult newResult = new QueryResult();

    for (QueryColumn qc : old_qr.q_cols) {
      newResult.q_cols.add(qc.setName(newName));
    }
    newResult.names.add(newName);

    return newResult;
  }

  public static List<String> getNames(QueryResult qr) {
    return qr.getNames();
  }

  public List<String> getNames() {
    return names;
  }

  public static boolean containsName(QueryResult qr, String name) {
    return getNames(qr).contains(name);
  }

  public static List<QueryColumn> getQueryColumns(final QueryResult qr) {
    return qr.q_cols;
  }

  private List<String> names;
  private List<QueryColumn> q_cols;

  private QueryResult() {
    this(new ArrayList<>(), new ArrayList<>());
  }

  private QueryResult(List<String> names, List<QueryColumn> q_cols) {
    this.names = names;
    this.q_cols = q_cols;
  }

}
