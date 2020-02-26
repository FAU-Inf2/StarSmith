package runtime;

public class UsableColumn {

  public static final UsableColumn getNew(QueryColumn col, final boolean scalarPos,
      final boolean groupPos) {
    return new UsableColumn(col, scalarPos, groupPos, false);
  }

  public static final UsableColumn getNew(QueryColumn col, final boolean scalarPos,
      final boolean groupPos, final boolean isFromOutsideCurrentQuery) {
    return new UsableColumn(col, scalarPos, groupPos, isFromOutsideCurrentQuery);
  }

  public static final QueryColumn getQueryColumn(UsableColumn uc) {
    return uc.column;
  }

  public static final String getName(final UsableColumn uc) {
    return uc.getName();
  }

  public final String getName() {
    return column.getFullName();
  }

  public static final Type getType(final UsableColumn uc) {
    return uc.column.getType();
  }

  public static final boolean canBeUsedScalar(final UsableColumn uc) {
    return uc.scalarPos;
  }

  public static final boolean canBeUsedGrouped(final UsableColumn uc) {
    return uc.groupPos;
  }

  public static final boolean isFromOutsideCurrentQuery(final UsableColumn uc) {
    return uc.fromOutsideCurrentQuery;
  }

  private QueryColumn column;
  private boolean scalarPos;
  private boolean groupPos;
  private boolean fromOutsideCurrentQuery;

  private UsableColumn(final QueryColumn column,
      final boolean scalarPos, final boolean groupPos,
      final boolean fromOutsideCurrentQuery) {
    this.column = column;
    this.scalarPos = scalarPos;
    this.groupPos = groupPos;
    this.fromOutsideCurrentQuery = fromOutsideCurrentQuery;
  }

}
