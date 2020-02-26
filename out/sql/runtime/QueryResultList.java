package runtime;

import java.util.List;
import java.util.ArrayList;

import java.util.stream.Collectors;


public class QueryResultList {

  public static QueryResultList getEmpty() {
    return new QueryResultList();
  }

  public static QueryResultList add(final QueryResultList qrl, final QueryResult qr) {
    QueryResultList newqrl = qrl.clone();
    newqrl.results.add(qr);
    return newqrl;
  }

  public static boolean containsName(final QueryResultList qrl, final String name) {
    for (QueryResult qr : qrl.results) {
      if (QueryResult.getNames(qr).contains(name)) {
        return true;
      }
    }

    return false;
  }

  public static UsableColumnList getColumnsAsUsableForWhereClause(QueryResultList qrl) {
    List<UsableColumn> luc = new ArrayList<>();

    for (QueryResult result : qrl.results) {
      List<UsableColumn> vars_from_single_result =
          QueryResult.getQueryColumns(result).stream().map(
              qc -> UsableColumn.getNew(qc, true, false)).collect(Collectors.toList());
      luc.addAll(vars_from_single_result);
    }

    return UsableColumnList.getFromList(luc);
  }

  public static boolean mustForbidAggregateFunctions(GroupedColumnList gcl, boolean scalarDecider) {
    boolean isGroupedAtAll = GroupedColumnList.isGroupedAtAll(gcl);
    boolean mustForbid = (!isGroupedAtAll && scalarDecider);
    return mustForbid;
  }

  public static UsableColumnList getColumnsAsUsableForSelectClause(QueryResultList qrl,
      GroupedColumnList gcl, boolean scalarDecider){
    List<UsableColumn> luc = new ArrayList<>();

    boolean isGroupedAtAll = GroupedColumnList.isGroupedAtAll(gcl);

    for (QueryResult result : qrl.results) {
      List<UsableColumn> vars_from_single_result = QueryResult.getQueryColumns(result).stream().map(qc ->
      {
        if(!isGroupedAtAll){
          return UsableColumn.getNew(qc, scalarDecider, !scalarDecider);
        }

        boolean isSingleGroup = GroupedColumnList.getSingleGroup(gcl);
        if(isSingleGroup){
          return UsableColumn.getNew(qc, false, true);
        }

        boolean wasGroupedBefore = gcl.contains(qc);
        return UsableColumn.getNew(qc, wasGroupedBefore, true);

      }).collect(Collectors.toList());

      luc.addAll(vars_from_single_result);
    }

    // now it's grouped, so it can be used like above.
    return UsableColumnList.getFromList(luc);
  }

  public static UsableColumnList getColumnsAsUsableForJoinClause(QueryResultList qrl) {
    return getColumnsAsUsableForWhereClause(qrl);
  }

  public static UsableColumnList getColumnsAsUsableForHavingClause(QueryResultList qrl,
      GroupedColumnList gcl) {
    List<UsableColumn> luc = new ArrayList<>();

    for(QueryResult result : qrl.results) {
      List<UsableColumn> vars_from_single_result = QueryResult.getQueryColumns(result).stream().map(qc ->
      {
        boolean wasGroupedBefore = gcl.contains(qc);
        return UsableColumn.getNew(qc, wasGroupedBefore, true);
      }).collect(Collectors.toList());

      luc.addAll(vars_from_single_result);
    }

    return UsableColumnList.getFromList(luc);
  }


  public static UsableColumnList getColumnsAsUsableForGroupByClause(QueryResultList qrl) {
    return getColumnsAsUsableForWhereClause(qrl);
  }

  public static UsableColumnList getColumnsAsUsableForOrderByClause(QueryResultList qrl,
      GroupedColumnList gcl, boolean scalarDecision) {
    UsableColumnList fromTablequerying =
        getColumnsAsUsableForSelectClause(qrl, gcl, scalarDecision);

    return fromTablequerying;
  }

  private List<QueryResult> results;

  public QueryResultList() {
    results = new ArrayList<>();
  }

  protected QueryResultList clone() {
    QueryResultList nq = new QueryResultList();
    nq.results = new ArrayList<>(results);
    return nq;
  }

}
