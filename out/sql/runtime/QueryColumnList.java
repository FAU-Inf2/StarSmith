package runtime;

import java.util.List;
import java.util.ArrayList;

import i2.act.fuzzer.Node;

public class QueryColumnList extends ROL<QueryColumn> {

  public static final QueryColumnList getEmpty() {
    return new QueryColumnList();
  }

  public QueryColumnList getNew() {
    return getEmpty();
  }
 
  public static QueryColumnList getFromList(List<QueryColumn> curlist) {
    QueryColumnList qcl = getEmpty();
    qcl.list = curlist;
    return qcl;
  }

  public List<QueryColumn> getQueryColumns() {
    return list;
  }

  public static QueryColumn getRandomElement (QueryColumnList qcl, final Node node) {
    return Random.getRandomFromList(qcl.list, node);
  }

  public boolean equalsAccordingToList(QueryColumn a, QueryColumn b) {
    return a.getFullName().equals(b.getFullName());
  }

  public static QueryColumnList removeUniqueQuantifier(QueryColumnList qcl) {
    List<QueryColumn> newlist = new ArrayList<QueryColumn>();

    for (QueryColumn qc : qcl.list) {
      newlist.add(qc.setUnique(false));
    }

    return QueryColumnList.getFromList(newlist);
  }
}
