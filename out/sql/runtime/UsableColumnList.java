package runtime;

import i2.act.fuzzer.Node;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class UsableColumnList extends ROL<UsableColumn> {

  public static final UsableColumnList getEmpty() {
    return new UsableColumnList();
  }

  public UsableColumnList getNew() {
    return getEmpty();
  }

  public static final UsableColumnList getFromList(List<UsableColumn> lst) {
    UsableColumnList ucl = new UsableColumnList();
    ucl.list.addAll(lst);
    return ucl;
  }

  public static UsableColumnList mergeOutsideAndCurrent(UsableColumnList outside,
      UsableColumnList current) {
    List<UsableColumn> cols = new ArrayList<>();

    HashSet<String> currentQualNames = new HashSet<>();
    current.getAll().stream().forEach(uc -> {
      currentQualNames.add(uc.getName().split("\\.")[0]);
    });

    for (UsableColumn out : outside.getAll()) {
      if (!currentQualNames.contains(out.getName().split("\\.")[0])) {
        cols.add(UsableColumn.getNew(
            UsableColumn.getQueryColumn(out),
            UsableColumn.canBeUsedScalar(out),
            UsableColumn.canBeUsedGrouped(out),
            true
        ));
      }
    }

    cols.addAll(current.getAll().stream().map(uc -> UsableColumn.getNew(
            UsableColumn.getQueryColumn(uc),
            UsableColumn.canBeUsedScalar(uc),
            UsableColumn.canBeUsedGrouped(uc)

            )).collect(Collectors.toList()));

    UsableColumnList erg = getFromList(cols);

    return erg;
  }

  public static UsableColumnList filterBasedOnEnvironment(UsableColumnList ucl,
      boolean inWhereOfSDU, String compOp) {
    //in a where clause, columns from outside are allowed if used in an equal operator
    //otherwise, they are not allowed!
    if(inWhereOfSDU && "=".equals(compOp)){ //currently in a where-clause and equal
      return ucl; // also outside columns are usable
    } else { //if no
      return getOnlyInside(ucl);
    }
  }

  public static UsableColumnList getOnlyInside(UsableColumnList ucl) {
    List<UsableColumn> ucls = ucl.getAll().stream().filter(uc ->
        !UsableColumn.isFromOutsideCurrentQuery(uc))
    .map(uc -> UsableColumn.getNew(
            UsableColumn.getQueryColumn(uc),
            UsableColumn.canBeUsedScalar(uc),
            UsableColumn.canBeUsedGrouped(uc),
            false
      )).collect(Collectors.toList());

    return getFromList(ucls);
  }

  public static UsableColumnList mergeTableAndSelectList(UsableColumnList tables,
      UsableColumnList selectlist){
    List<UsableColumn>  ucls = new ArrayList<>();
    ucls.addAll(tables.list);
    ucls.addAll(selectlist.list);

    return getFromList(ucls);
  }

  private static List<UsableColumn> filterForType(UsableColumnList ucl, Type t) {
    return ucl.list.stream().filter(l -> {
       return Type.isImplicitConvertible(UsableColumn.getType(l), t);
      }
    ).collect(Collectors.toList());
  }

  public static final UsableColumn getRandomWithType(UsableColumnList ucl, final Node node,
      Type t) {
    List<UsableColumn> vars_with_type = filterForType(ucl, t);
    if (vars_with_type.isEmpty()) {
        throw new RuntimeException("Should not be empty, must be checked before!");
    }
    return ListUtil.chooseRandom(vars_with_type, node);
  }

  public static final boolean hasObjectWithType(UsableColumnList ucl, Type t){
    List<UsableColumn> vars_with_type = filterForType(ucl, t);
    return !vars_with_type.isEmpty();
  }

  public static final UsableColumnList getScalarUsableColumns(UsableColumnList ucl) {
    return getFromList(ucl.list.stream().filter(
        uc -> UsableColumn.canBeUsedScalar(uc)).collect(Collectors.toList()));
  }

  public static final UsableColumnList getGroupedUsableColumns(UsableColumnList ucl) {
    return getFromList(ucl.list.stream().filter(
        uc -> UsableColumn.canBeUsedGrouped(uc)).collect(Collectors.toList()));
  }

  public boolean equalsAccordingToList(UsableColumn a, UsableColumn b) {
    return a.getName().equals(b.getName());
  }

  public static final UsableColumnList addScalarPropertyToAll(UsableColumnList ucl) {
    return getFromList(ucl.list.stream().map(uc -> UsableColumn.getNew(
            UsableColumn.getQueryColumn(uc),
            true,
            UsableColumn.canBeUsedGrouped(uc)
            )).collect(Collectors.toList()));
  }

  public static final QueryColumnList transformToQueryColumnsForWindowOrderByFunction(
      UsableColumnList ucl) {
    //currently, ohne scalar usable columns are valid!
    UsableColumnList scalarUsable = getScalarUsableColumns(ucl);
    ArrayList<QueryColumn> qcl = new ArrayList<>();
    for (UsableColumn uc : ucl.list) {
      qcl.add(UsableColumn.getQueryColumn(uc));
    }

    return QueryColumnList.getFromList(qcl);
  }

}

