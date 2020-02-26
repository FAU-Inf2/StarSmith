use StdLib;
use IdentifierBuilder;
use UsableColumnList;
use SchemaSet;
use Schema;
use Table;
use TableColumns;
use TableColumn;
use Type;
use NumericType;
use ListUtil;
use QueryResultList;
use QueryResult;
use QueryColumn;
use QueryColumnList;
use UsableColumn;
use GroupedColumnList;
use Utils;
use Random;
use ExpectedSelectList;
use View;
use Queryable;
use NameTypePair;
use SortElement;
use SortElementList;

class Program {

  program ("${statements : StatementList}\n") {
    statements.schemas_before = (SchemaSet:emptySet);
  }

}

@list(500)
class StatementList {

  inh schemas_before : SchemaSet;
  syn schemas_after : SchemaSet;

  @weight(1)
  one_stmt ("${stmt : Statement};\n"){
    stmt.schemas_before = this.schemas_before;
    this.schemas_after = stmt.schemas_after;
  }

  @weight(90)
  multiple_stmts ("${stmts : StatementList}\n${stmt : Statement};\n") {
    stmts.schemas_before = this.schemas_before;
    stmt.schemas_before = stmts.schemas_after;
    this.schemas_after = stmt.schemas_after;
  }

}

@unit
class Statement {

  inh schemas_before : SchemaSet;
  syn schemas_after : SchemaSet;

  @weight(1)
  @copy(schemas_before, schemas_after)
  schema_definition ("${sd : SchemaDefinition}") { }

  @weight(5)
  table_definition ("${td : TableDefinition}") {
    loc modified_schema_name = (Table:getSchemaName td.newtable);
    loc to_modify_schema =  (SchemaSet:getSchemaByName this.schemas_before .modified_schema_name);
    loc modified_schema = (Schema:addTable .to_modify_schema td.newtable);

    this.schemas_after = (SchemaSet:add this.schemas_before .modified_schema);
    td.allowed_schemas = this.schemas_before;
  }

  @weight(25)
  insert_statement ("${is : InsertStatement}") {
    is.schemas_before = this.schemas_before;
    this.schemas_after = this.schemas_before;
  }

  @weight(50)
  query_specification ("${rqe : RandomQueryExpression};\n${sei : SelectExtraInformations}") {
    rqe.schemas_before = this.schemas_before;
    rqe.isOnTopLevel = true;
    rqe.inInsertStatement = false;
    this.schemas_after = this.schemas_before;
    sei.sort_elem_list = rqe.sort_elem_list;
    sei.limit_count = rqe.limit_count;
  }

  @weight(5)
  view_definition ("${vd : ViewDefinition}") {
    loc mofified_schema_name = (View:getSchemaName vd.newview);
    loc to_modify_schema =  (SchemaSet:getSchemaByName this.schemas_before .mofified_schema_name);
    loc modified_schema = (Schema:addView .to_modify_schema vd.newview);

    this.schemas_after = (SchemaSet:add this.schemas_before .modified_schema);

    vd.schemas = this.schemas_before;
  }

}

class SelectExtraInformations {

  inh sort_elem_list : SortElementList;
  inh limit_count : int;

  select_extra ("-- { 'uniq_ordering' : #{(SortElementList:isUniqueOrdering this.sort_elem_list)}, 'ordering' : #{(SortElementList:print this.sort_elem_list)}, 'limit' : #{this.limit_count} } ") { }
}

class ViewDefinition {

  inh schemas : SchemaSet;

  syn newview : View;

  grd allow;

  vd ("CREATE VIEW ${tn : TableName} AS ${rl : ConstructRandomExpectedSelectList}${qe : QueryExpression}") {
    loc chosen_schema_name = (SchemaSet:getRandomSchemaName this.schemas this);
    loc chosen_schema = (SchemaSet:getSchemaByName this.schemas .chosen_schema_name);

    this.allow = (SchemaSet:containsTables this.schemas);

    qe.expected_selectlist = rl.constructed_selectlist;
    qe.schemas_before = this.schemas;
    qe.usable_columns = (UsableColumnList:getEmpty);
    qe.mustBeScalar = false;
    qe.inInsertStatement = false;
    qe.isOnTopLevel = true;

    this.newview = (View:getNew tn.table_name qe.selected_query_columns .chosen_schema_name);

    tn.schema = .chosen_schema;
  }

}

class RandomQueryExpression {

  inh schemas_before : SchemaSet;

  inh isOnTopLevel : boolean;
  inh inInsertStatement : boolean;

  syn selected_query_columns : QueryColumnList;
  syn sort_elem_list : SortElementList;
  syn limit_count : int;

  grd allowed;

  @copy(schemas_before, isOnTopLevel, inInsertStatement, selected_query_columns, sort_elem_list, limit_count)
  indirection ("${rl : ConstructRandomExpectedSelectList}${qe : QueryExpression}") {
    qe.expected_selectlist = rl.constructed_selectlist;
    qe.usable_columns = (UsableColumnList:getEmpty);
    qe.mustBeScalar = false; # is random
    this.allowed = (SchemaSet:containsQueryable this.schemas_before);
  }

}

class ConstructRandomExpectedSelectList {

  syn constructed_selectlist : ExpectedSelectList;

  help ("${creslh : ConstructRandomExpectedSelectListHelper}") {
    creslh.constructed_selectlist_before = (ExpectedSelectList:getEmpty);
    this.constructed_selectlist = creslh.constructed_selectlist_after;
  }

}

@list(10)
class ConstructRandomExpectedSelectListHelper {

  inh constructed_selectlist_before : ExpectedSelectList;
  syn constructed_selectlist_after  : ExpectedSelectList;

  one ("${hdti : HiddenDataTypeIdentifier}") {
    loc newobj = (NameTypePair:getNew hdti.name hdti.type);
    hdti.invalidNames = (ExpectedSelectList:getNames this.constructed_selectlist_before);
    this.constructed_selectlist_after = (ExpectedSelectList:add this.constructed_selectlist_before .newobj);
  }

  @weight(2)
  multiple ("${creslh : ConstructRandomExpectedSelectListIndirection}${hdti : HiddenDataTypeIdentifier}") {
    loc newobj = (NameTypePair:getNew hdti.name hdti.type);
    creslh.constructed_selectlist_before = this.constructed_selectlist_before;
    hdti.invalidNames = (ExpectedSelectList:getNames creslh.constructed_selectlist_after);
    this.constructed_selectlist_after = (ExpectedSelectList:add creslh.constructed_selectlist_after .newobj);
  }

}

@copy
class ConstructRandomExpectedSelectListIndirection {

  inh constructed_selectlist_before : ExpectedSelectList;
  syn constructed_selectlist_after  : ExpectedSelectList;

  cresl ("${creslh : ConstructRandomExpectedSelectListHelper}") { }

}


class HiddenDataTypeIdentifier {

  inh invalidNames : List; #list of str

  syn type: Type;
  syn name: String;

  grd nameValid;

  hdthi ("${hdt : HiddenDataType}${hi : HiddenIdentifier}") {
    this.type = hdt.randomSuperType;
    this.name = hi.name;
    this.nameValid = (not (ListUtil:contains this.invalidNames hi.name));
  }

}

@unit
class QuerySpecification {

  inh expected_selectlist : ExpectedSelectList;
  inh schemas_before : SchemaSet;

  inh usable_columns_outside : UsableColumnList;

  inh isOnTopLevel : boolean; # needed for limit currently.
  inh inInsertStatement : boolean;

  inh mustBeScalar : boolean; # needed for scalar subquery
  # Scalarity is enforced in these ways:
  # - force ordering by something (which is always the first column)
  # - limit by 1

  syn selected_query_columns : QueryColumnList;

  grd allow;

  @copy(schemas_before, mustBeScalar, isOnTopLevel, inInsertStatement, expected_selectlist,
    usable_columns_outside, selected_query_columns)
  sel ("${sd : ConstructScalarDecider}SELECT ${osq : OptionalSetQuantifier} ${sl : SelectList} ${te : TableExpression}") {
    this.allow = (SchemaSet:containsQueryable this.schemas_before);

    sl.query_results = te.query_results;
    sl.grouped_columns = te.grouped_columns;
    sl.scalarDecision = sd.scalarDecision;
    osq.allowDistinct = true;
  }

}

# Note: The standard has a syntax which is not supported by most databases.
# Thus, the postgres syntax (which is used by other databases as well) is used.
class OptionalFetchFirstOffsetClause {

  inh isOnTopLevel : boolean;
  inh mustBeScalar : boolean;
  inh ordered : boolean;
  inh isUniquelyOrdered : boolean;

  syn limit_count : int;

  grd allowed;

  none ("") {
    this.allowed = (not this.mustBeScalar);
    this.limit_count = -1;
  }

  @copy(mustBeScalar, limit_count)
  ff ("${ffc : FetchFirstOffsetClause}") {
    # Fetch First only on toplevel, otherwise results are not deterministic.

    # if not ordered: no
    # if mustBeScalar: yes
    # if featureallowed:
    # if isOnTopLevel: yes
    # if unique ordered: yes

    this.allowed = (and this.ordered
      (or this.mustBeScalar
      (or this.isOnTopLevel this.isUniquelyOrdered)));
  }

}

# While the standard says that Offset could be used without limit, not all databases support this!
class FetchFirstOffsetClause {

  inh mustBeScalar : boolean;

  syn limit_count : int;

  @copy(mustBeScalar)
  ffooc ("LIMIT ${cc : ConstructCount} ${ooc : OptionalOffsetClause}") {
    this.limit_count = cc.count;
    cc.forceOne = this.mustBeScalar;
  }

}

class OptionalOffsetClause {

  inh mustBeScalar : boolean;

  grd allowed;

  none ("") {
    this.allowed = true;
  }

  oc ("${oc : OffsetClause}") {
    this.allowed = this.mustBeScalar;
  }

}

class OffsetClause {

  oc ("OFFSET ${cc : ConstructCount}") {
    cc.forceOne = false;
  }

}

@copy(count)
class ConstructCount {

  inh forceOne : boolean;

  syn count : int;

  grd allowed;

  one ("1") {
    this.count = 1;
    this.allowed = true;
  }

  @weight(5)
  sc ("${sc : SmallCount}") {
    this.allowed = (not this.forceOne);
  }

  @weight(2)
  nc ("${sc : NormalCount}") {
    this.allowed = (not this.forceOne);
  }

  @weight(1)
  bc ("${bc : BigCount}") {
    this.allowed = (not this.forceOne);
  }

}

class SmallCount {

  syn count : int;

  sm ("${sm : SMALLINT}") {
    this.count = (StdLib:toInt sm.str);
  }

}

class NormalCount {

  syn count : int;

  nc ("${nc : NORMALINT}") {
    this.count = (StdLib:toInt nc.str);
  }

}

class BigCount {

  syn count : int;

  nc ("${bc : BIGINT}") {
    this.count = (StdLib:toInt bc.str);
  }

}

class OptionalSetQuantifier {

  inh allowDistinct : boolean;

  @weight(5)
  nope ("") { }

  @copy
  sq ("${sq : SetQuantifier}") { }

}

class SetQuantifier {

  inh allowDistinct : boolean;

  grd allowed;

  distinct ("DISTINCT") {
    this.allowed = this.allowDistinct;
  }

  all ("ALL") {
    this.allowed = true;
  }

}

class SelectList {

  inh expected_selectlist : ExpectedSelectList;

  inh schemas_before : SchemaSet;

  inh query_results : QueryResultList;
  inh grouped_columns : GroupedColumnList;

  inh scalarDecision : boolean;
  inh inInsertStatement : boolean;

  syn selected_query_columns : QueryColumnList;

  grd allowed;

  @weight(20)
  @copy(query_results, grouped_columns, schemas_before, scalarDecision, inInsertStatement, expected_selectlist)
  subset_random ("${slh : SelectListHelper}") {
    slh.query_columns_before = (QueryColumnList:getEmpty);

    this.selected_query_columns = slh.query_columns_after;

    slh.cur_index = (StdLib:sub (ExpectedSelectList:size this.expected_selectlist) 1);

    this.allowed = true;
  }

}

@list
@copy(schemas_before, query_results, grouped_columns, scalarDecision, inInsertStatement)
class SelectListHelper {

  inh schemas_before : SchemaSet;

  inh expected_selectlist : ExpectedSelectList;
  inh cur_index : int;

  inh query_results : QueryResultList;
  inh grouped_columns : GroupedColumnList;

  inh scalarDecision : boolean;
  inh inInsertStatement : boolean;

  inh query_columns_before : QueryColumnList;
  syn query_columns_after : QueryColumnList;

  grd allowed;

  one ("${ssl : SelectSublist}") {
    loc curSelectElem = (ExpectedSelectList:get this.expected_selectlist this.cur_index);
    ssl.query_columns_before = this.query_columns_before;
    ssl.expected_type = (NameTypePair:getType .curSelectElem);
    ssl.expected_name = (NameTypePair:getName .curSelectElem);
    this.query_columns_after = ssl.query_columns_after;
    this.allowed = (StdLib:eq this.cur_index 0);
  }

  multiple ("${slh : SelectListHelper}, ${ssl : SelectSublist}") {
    loc curSelectElem = (ExpectedSelectList:get this.expected_selectlist this.cur_index);

    slh.query_columns_before = this.query_columns_before;
    ssl.query_columns_before = slh.query_columns_after;
    ssl.expected_type = (NameTypePair:getType .curSelectElem);
    ssl.expected_name = (NameTypePair:getName .curSelectElem);
    this.query_columns_after = ssl.query_columns_after;

    this.allowed = (StdLib:ne this.cur_index 0);
    slh.cur_index = (StdLib:sub this.cur_index 1);
    slh.expected_selectlist = this.expected_selectlist;
  }

}

class SelectSublist {

  inh schemas_before : SchemaSet;
  inh expected_type : Type;
  inh expected_name : String;

  inh query_results : QueryResultList;
  inh grouped_columns : GroupedColumnList;

  inh query_columns_before : QueryColumnList;
  syn query_columns_after : QueryColumnList;
  inh scalarDecision : boolean;
  inh inInsertStatement : boolean;

  grd allowed;

  @copy(query_results, grouped_columns, schemas_before, expected_type, expected_name, scalarDecision, inInsertStatement)
  dc ("${dc : DerivedColumn}") {

    loc query_col = (QueryColumn:getNew dc.chosenname dc.chosentype dc.isUnique);

    # in a column, at first, not in a aggregatefunction
    dc.inAggregateFunction = false;
    dc.inSelectList = true;

    this.allowed = (not (QueryColumnList:contains this.query_columns_before .query_col));

    this.query_columns_after = (QueryColumnList:add this.query_columns_before .query_col);
  }

}

class DerivedColumn {

  inh schemas_before : SchemaSet;
  inh query_results : QueryResultList;
  inh grouped_columns : GroupedColumnList;

  inh expected_type : Type;
  inh expected_name : String;

  inh inAggregateFunction : boolean;
  inh scalarDecision : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn chosentype : Type;
  syn chosenname : String;
  syn isUnique : boolean;

  @copy(schemas_before, inInsertStatement, inSelectList, isUnique)
  ve ("${ve : ValueExpression} ${acc : AsClauseColumn}") {
    loc chos_type = this.expected_type;
    ve.expectedType = .chos_type;
    ve.usable_columns =
      (QueryResultList:getColumnsAsUsableForSelectClause
        this.query_results this.grouped_columns this.scalarDecision);
    ve.inWhereOfSDU = false;
    ve.inAggregateFunction = (or
      this.inAggregateFunction
      (QueryResultList:mustForbidAggregateFunctions this.grouped_columns this.scalarDecision));

    acc.expected_name = this.expected_name;
    this.chosentype = .chos_type;
    this.chosenname = this.expected_name;
  }

}

class ConstructScalarDecider {

  syn scalarDecision : boolean;

  @weight(5)
  tr ("") {
    this.scalarDecision = true;
  }

  fa ("") {
    this.scalarDecision = false;
  }

}

class AsClauseColumn {

  inh expected_name : String;

  syn name : String;

  id ("AS #{this.expected_name}") {
    this.name = this.expected_name;
  }

}

class TableExpression {

  inh schemas_before : SchemaSet;

  inh usable_columns_outside : UsableColumnList;

  inh mustBeScalar : boolean;
  inh inInsertStatement : boolean;

  syn query_results : QueryResultList;
  syn grouped_columns : GroupedColumnList;

  @copy(usable_columns_outside, schemas_before, inInsertStatement)
  from_clause ("${fc : FromClause} ${owc : OptionalWhereClause} ${ogbc : OptionalGroupByClause} ${ohc: OptionalHavingClause}") {
    owc.query_results = fc.query_results;
    ogbc.query_results = fc.query_results;

    ohc.query_results = fc.query_results;
    ohc.grouped_columns_before = ogbc.grouped_columns;
    ohc.wasExplicitGroupedBefore = (not (GroupedColumnList:isEmpty ogbc.grouped_columns));

    this.query_results = fc.query_results;
    this.grouped_columns = ohc.grouped_columns_after;
  }

}

@copy(selected_query_columns, schemas_before, sort_elem_list)
class OptionalOrderByClause {

  inh schemas_before : SchemaSet;

  inh selected_query_columns : QueryColumnList;

  inh mustBeScalar : boolean;

  syn sort_elem_list : SortElementList;

  grd scalarOrderGuard;

  none ("") {
    this.scalarOrderGuard = (not this.mustBeScalar); # not scalar
    this.sort_elem_list = (SortElementList:getEmpty);
  }

  # limit must activate order, otherwise nondeterministic!
  # subquery needs limit and therefore ordering!
  @weight(3)
  obc ("${obc : OrderByClause}") {
    this.scalarOrderGuard = true;
    obc.need_real_ordering = this.mustBeScalar;
  }

}

@copy(schemas_before, selected_query_columns, need_real_ordering)
class OrderByClause {

  inh schemas_before : SchemaSet;
  inh selected_query_columns : QueryColumnList;

  inh need_real_ordering : boolean;

  syn sort_elem_list : SortElementList;

  ssl ("ORDER BY ${ssl : SortSpecificationList}") {
    this.sort_elem_list = ssl.sort_elem_list;
    ssl.allow_numbered_ordering = true;
  }

}

@copy
class SortSpecificationList {

  inh schemas_before : SchemaSet;
  inh selected_query_columns : QueryColumnList;

  inh need_real_ordering : boolean;
  inh allow_numbered_ordering : boolean;

  syn sort_elem_list : SortElementList;

  sslh ("${sslh : SortSpecificationListHelper}") {
    sslh.sort_elem_list_before = (SortElementList:getEmpty);
    this.sort_elem_list = sslh.sort_elem_list_after;
  }

}

@list
@copy(schemas_before, selected_query_columns, need_real_ordering, sort_elem_list_before, allow_numbered_ordering)
class SortSpecificationListHelper {

  inh schemas_before : SchemaSet;
  inh selected_query_columns : QueryColumnList;

  inh need_real_ordering : boolean;
  inh allow_numbered_ordering : boolean;

  inh sort_elem_list_before : SortElementList;
  syn sort_elem_list_after : SortElementList;

  @weight(2)
  ss ("${ss : SortSpecification}") {
    this.sort_elem_list_after = (SortElementList:add_checked this.sort_elem_list_before ss.sort_elem);
  }

  sssslh ("${sslh : SortSpecificationListHelper}, ${ss : SortSpecification}") {
    sslh.sort_elem_list_before = this.sort_elem_list_before;
    this.sort_elem_list_after = (SortElementList:add_checked sslh.sort_elem_list_after ss.sort_elem);
  }

}

class SortSpecification {

  inh schemas_before : SchemaSet;
  inh selected_query_columns : QueryColumnList;

  inh need_real_ordering : boolean;
  inh allow_numbered_ordering : boolean;

  syn sort_elem : SortElement;

  @copy(schemas_before, selected_query_columns, need_real_ordering, allow_numbered_ordering)
  skoosono ("${sk : SortKey} ${oos : OptionalOrderingSpecification} ${ono : OptionalNullOrdering}") {
    ono.order_asc = oos.order_asc;
    this.sort_elem = (SortElement:getNew sk.index oos.order_asc ono.null_first sk.unique);
  }

}

class SortKey {

  inh schemas_before : SchemaSet;
  inh selected_query_columns : QueryColumnList;

  inh need_real_ordering : boolean;
  inh allow_numbered_ordering : boolean;

  syn index : int;
  syn unique : boolean;

  grd allowed;

  @weight(2)
  @copy(index, selected_query_columns, unique)
  gsi ("${gsi : GroupSelectIndex}") { # use correct numbers from length of selectlist
    this.allowed = this.allow_numbered_ordering;
  }

  @weight(4)
  @copy(index, selected_query_columns, unique)
  gse ("${gse : GroupSelectElement}") {
    this.allowed = true;
  }

}

class GroupSelectIndex {

  inh selected_query_columns : QueryColumnList;

  syn index : int;
  syn unique : boolean;

  rnd ("#{.chosen_elem_index}") {
    loc chosen_elem_index =
      (Random:getRandomInBounds
        1 (StdLib:add (QueryColumnList:size this.selected_query_columns) 1) this);
    loc qc = (QueryColumnList:get this.selected_query_columns (StdLib:sub .chosen_elem_index 1));
    this.unique = (QueryColumn:isUnique .qc);
    this.index = .chosen_elem_index;
  }

}

class GroupSelectElement {

  inh selected_query_columns : QueryColumnList;

  syn index : int;
  syn unique : boolean;

  rnd ("#{(QueryColumn:getFullName .qc)}") {
    loc chosen_elem_index =
      (Random:getRandomInBounds
        1 (StdLib:add (QueryColumnList:size this.selected_query_columns) 1) this);
    loc qc = (QueryColumnList:get this.selected_query_columns (StdLib:sub .chosen_elem_index 1));
    this.unique = (QueryColumn:isUnique .qc);
    this.index = .chosen_elem_index;
  }

}

class OptionalOrderingSpecification {

  syn order_asc : boolean;

  none ("") {
    this.order_asc = true;
  }

  @copy(order_asc)
  os ("${os : OrderingSpecification}") { }

}

class OrderingSpecification {

  syn order_asc : boolean;

  asc ("ASC") {
    this.order_asc = true;
  }

  desc ("DESC") {
    this.order_asc = false;
  }

}


class OptionalNullOrdering {

  inh order_asc : boolean;

  syn null_first : boolean;

  # none ("") { } // behavior not defined in standard!

  @copy(null_first, order_asc)
  no ("${no : NullOrdering}") { }

}

class NullOrdering {

  inh order_asc : boolean;

  syn null_first : boolean;

  grd allowed;

  fst ("NULLS FIRST") {
    this.null_first = true;
    this.allowed = this.order_asc;
  }

  lst ("NULLS LAST") {
    this.null_first = false;
    this.allowed = (not this.order_asc);
  }

}


class OptionalGroupByClause {

  inh query_results : QueryResultList;
  inh schemas_before : SchemaSet;

  syn grouped_columns : GroupedColumnList;

  @weight(3)
  none ("") {
    this.grouped_columns = (GroupedColumnList:getEmpty);
  }

  groupby_clause ("${gb : GroupByClause}") {
    gb.query_results = this.query_results;
    gb.schemas_before = this.schemas_before;
    this.grouped_columns = gb.grouped_columns;
  }

}

class GroupByClause {

  inh query_results : QueryResultList;
  inh schemas_before : SchemaSet;

  syn grouped_columns : GroupedColumnList;

  groupby_clause ("GROUP BY ${gel : GroupedColumnList}") {
    gel.query_results = this.query_results;
    gel.grouped_columns_before = (GroupedColumnList:getEmpty);
    this.grouped_columns = gel.grouped_columns_after;
  }

}

class OptionalHavingClause {

  inh schemas_before : SchemaSet;

  inh query_results : QueryResultList;

  inh grouped_columns_before : GroupedColumnList;
  syn grouped_columns_after : GroupedColumnList;

  inh inInsertStatement : boolean;
  inh wasExplicitGroupedBefore : boolean;

  @weight(5)
  none ("") {
    this.grouped_columns_after = this.grouped_columns_before;
  }

  @copy(schemas_before, query_results, inInsertStatement)
  having_clause ("${hc : HavingClause}") {
    hc.grouped_columns = this.grouped_columns_before;
    this.grouped_columns_after =
      (GroupedColumnList:setSingleGroup
        this.grouped_columns_before (not this.wasExplicitGroupedBefore));
  }

}

class HavingClause {

  inh schemas_before : SchemaSet;

  inh query_results : QueryResultList;
  inh grouped_columns : GroupedColumnList;
  inh inInsertStatement : boolean;

  @copy(schemas_before, inInsertStatement)
  having ("HAVING ${sc : SearchCondition}") {
    sc.usable_columns =
      (QueryResultList:getColumnsAsUsableForHavingClause this.query_results this.grouped_columns);
    sc.inWhereOfSDU = false;
    sc.inAggregateFunction = false;
    sc.inSelectList = false;
  }

}

@list
@copy(query_results)
class GroupedColumnList {

  inh query_results : QueryResultList;

  inh grouped_columns_before : GroupedColumnList;
  syn grouped_columns_after : GroupedColumnList;

  one_elem ("${ge : GroupingElement}") {
    this.grouped_columns_after = (GroupedColumnList:add this.grouped_columns_before ge.group_elem);
  }

  mul_elem ("${gel : GroupedColumnList}, ${ge : GroupingElement}") {
    gel.grouped_columns_before = this.grouped_columns_before;
    this.grouped_columns_after = (GroupedColumnList:add gel.grouped_columns_after ge.group_elem);
  }

}


class GroupingElement {

  inh query_results : QueryResultList;

  syn group_elem : QueryColumn;

  cr ("${cr : ColumnReference}") {
    cr.usable_columns = (QueryResultList:getColumnsAsUsableForGroupByClause this.query_results);
    cr.expectedType = (Type:getSUPERTYPE);
    this.group_elem = cr.chosen_tablecolumn;
  }

}

@copy(query_results, schemas_before, usable_columns_outside, inInsertStatement)
class OptionalWhereClause {

  inh query_results : QueryResultList;
  inh schemas_before : SchemaSet;
  inh usable_columns_outside : UsableColumnList;
  inh inInsertStatement : boolean;

  none ("") { }

  @weight(10)
  where ("WHERE ${wc : WhereClause}") {
    wc.inSelectList = false;
  }

}

@copy(schemas_before, inInsertStatement, inSelectList)
class WhereClause {

  inh query_results : QueryResultList;
  inh schemas_before : SchemaSet;
  inh usable_columns_outside : UsableColumnList;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  wsc ("${wsc : SearchCondition}") {
    wsc.usable_columns = (QueryResultList:getColumnsAsUsableForWhereClause this.query_results);
    wsc.inWhereOfSDU = true;
    wsc.inAggregateFunction = false;
  }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class SearchCondition {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  bve ("${bve : BooleanValueExpression}") { }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class BooleanValueExpression {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @weight(10)
  @copy(isUnique, isConstant)
  boolterm ("${bt : BooleanTerm}") { }

  bve_or_boolter ("${bve : BooleanValueExpression} OR ${bt : BooleanTerm}") {
    this.isConstant = (and bve.isConstant bt.isConstant);
    this.isUnique = (or (and bve.isConstant bt.isUnique) (and bve.isUnique bt.isConstant));
  }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class BooleanTerm {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @weight(10)
  @copy(isUnique, isConstant)
  boolfactor ("${bf : BooleanFactor}") { }

  boolterm_and_boolfactor ("${bt : BooleanTerm} AND ${bf : BooleanFactor}") {
    this.isConstant = (and bt.isConstant bf.isConstant);
    this.isUnique = (or (and bt.isConstant bf.isUnique) (and bt.isUnique bf.isConstant));
  }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList,
  isUnique, isConstant)
class BooleanFactor {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  on_booltest ("${onot : OptionalNot} ${bt : BooleanTest}") { }

}

class OptionalNot {

  @weight(10)
  none ("") { }

  not ("NOT") { }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class BooleanTest {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy(isUnique, isConstant)
  bt ("${bp : BooleanPrimary}") { }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class BooleanPrimary {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy(isUnique, isConstant)
  predicate ("${p : Predicate}") { }

  @copy(isUnique, isConstant)
  bool_pred ("${bp : BooleanPredicand}") { }

}

@copy
class BooleanPredicand {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  parent_bool_val_expr ("${pbvl : ParenthesizedBooleanValueExpression}") { }

  npvep ("${npvep : NonparenthesizedValueExpressionPrimary}") {
    npvep.expectedType = (Type:getBOOLEAN);
    this.isConstant = false;
  }

}

@copy
class ParenthesizedBooleanValueExpression {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  par_boolvalexpr ("(${bve : BooleanValueExpression})") { }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction)
class Predicate {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  grd allowed;

  @copy(inInsertStatement, inSelectList, isConstant)
  comparison_predicate ("${cp : ComparisonPredicate}") {
    this.allowed = true;
    this.isUnique = false; # passive approximation
  }

  @copy(inInsertStatement, inSelectList)
  exists_predicate ("${ep : ExistsPredicate}") {
    this.allowed = (and
      (SchemaSet:containsTables this.schemas_before)
      (not this.inInsertStatement));
    this.isUnique = false;
    # tracing would be possible here, but correlated querys would destroy it, so just no.
    this.isConstant = false;
  }

  @copy(inInsertStatement, inSelectList, isConstant)
  in_predicate ("${ip : InPredicate}") {
    this.allowed = this.inWhereOfSDU;
    this.isUnique = false;
  }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class InPredicate {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isConstant : boolean;

  in_pred ("${rvp : RowValuePredicand} ${ipp : InPredicatePart2}${hdt : HiddenDataType}") {
    loc chos_type = hdt.randomSuperType;

    rvp.expectedType = .chos_type;
    ipp.expectedType = .chos_type;

    this.isConstant = (and rvp.isConstant ipp.isConstant);
  }

}


@copy
class InPredicatePart2 {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;
  inh expectedType : Type;

  inh inSelectList : boolean;
  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;

  syn isConstant : boolean;

  ipp ("${on : OptionalNot} IN ${ipv : InPredicateValue}") { }

}

@copy(schemas_before, usable_columns, inInsertStatement)
class InPredicateValue {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;
  inh expectedType : Type;

  inh inSelectList : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;

  syn isConstant : boolean;

  @copy(schemas_before, expectedType, usable_columns, inSelectList, inAggregateFunction, isConstant)
  ivl ("( ${ivl : InValueList} )") { }

}

@copy
class InValueList {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;
  inh expectedType : Type;

  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isConstant : boolean;

  inlh ("${ivlh : InValueListHelper}") {
    ivlh.isConstant_before = true;
    this.isConstant = ivlh.isConstant_after;
  }

}

@list
@copy(usable_columns, schemas_before, expectedType, inAggregateFunction, inInsertStatement, inSelectList)
class InValueListHelper {

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;
  inh expectedType : Type;

  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh isConstant_before : boolean;
  syn isConstant_after : boolean;

  rve ("${rve : RowValueExpression}") {
    this.isConstant_after = (and rve.isConstant this.isConstant_before);
  }

  rveivlh ("${rve : RowValueExpression}, ${ivl : InValueListHelper}") {
    ivl.isConstant_before = this.isConstant_before;
    this.isConstant_after = (and ivl.isConstant_after rve.isConstant);
  }

}

@copy
class RowValueExpression {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh expectedType : Type;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isConstant : boolean;

  rvsc ("${rvsc : RowValueSpecialCase}") { }

}

class ExistsPredicate {

  inh schemas_before : SchemaSet;

  inh inInsertStatement : boolean;

  @copy(inInsertStatement)
  ts ("EXISTS ${rl : ConstructRandomExpectedSelectList}${ts : TableSubquery}") {
    ts.schemas_before = this.schemas_before;
    ts.expected_selectlist = rl.constructed_selectlist;
    ts.usable_columns = (UsableColumnList:getEmpty);
  }

}

@copy(schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class ComparisonPredicate {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isConstant : boolean;

  rvp("${rvp : RowValuePredicand} ${co : CompOp} ${rvp2 : RowValuePredicand}${hdt : HiddenDataType}"){
    loc rand_type = hdt.randomSuperType;
    loc local_usable_columns =
      (UsableColumnList:filterBasedOnEnvironment
        this.usable_columns this.inWhereOfSDU co.chosenCompOp);

    rvp.usable_columns = .local_usable_columns;
    rvp.expectedType = .rand_type;

    rvp2.usable_columns = .local_usable_columns;
    rvp2.expectedType = .rand_type;

    this.isConstant = (and rvp.isConstant rvp2.isConstant);
  }

}

@copy
class RowValuePredicand {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh expectedType : Type;
  syn constructedType : Type;

  syn isConstant : boolean;

  rvcp ("${rvcp : RowValueConstructorPredicand}") { }

  rvsc ("${rvsc : RowValueSpecialCase}") { }

}

class RowValueSpecialCase {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh expectedType : Type;
  syn constructedType : Type;

  syn isConstant : boolean;

  @copy(usable_columns, expectedType, schemas_before, inAggregateFunction, inInsertStatement,
    inSelectList, isConstant, constructedType)
  nvep ("${nvep : NonparenthesizedValueExpressionPrimary}") { }

}

class ColumnReference {

  inh usable_columns : UsableColumnList;
  inh expectedType : Type;

  syn chosen_tablecolumn : QueryColumn;
  syn constructedType : Type;
  syn isUnique : boolean;

  grd has_var_with_type;

  pr ("#{(QueryColumn:getFullName this.chosen_tablecolumn)}") {
    loc chos_col =
      (UsableColumn:getQueryColumn
        (UsableColumnList:getRandomWithType this.usable_columns this this.expectedType));

    this.has_var_with_type = (UsableColumnList:hasObjectWithType this.usable_columns this.expectedType);
    this.chosen_tablecolumn = .chos_col;
    this.constructedType = (QueryColumn:getType .chos_col);
    this.isUnique = (QueryColumn:isUnique .chos_col);
  }

}

@copy
class RowValueConstructorPredicand {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh expectedType : Type;
  syn constructedType : Type;

  syn isConstant : boolean;

  grd allowed;

  cve ("${cve : CommonValueExpression}") {
    this.allowed = true;
  }

  bp ("${bp : BooleanPredicand}") {
    this.allowed = (Type:isBooleanType this.expectedType);
    this.constructedType = (Type:getBOOLEAN);
  }

}

class CompOp {

  syn chosenCompOp : String;

  @weight(20)
  eq   ("=")  { this.chosenCompOp = "="; }

  @weight(2)
  neq  ("<>") { this.chosenCompOp = "<>"; }

  @weight(2)
  neq2 ("!=") { this.chosenCompOp = "<>"; } # semantically the same, so use <> for processing

  @weight(3)
  l    ("<")  { this.chosenCompOp = "<"; }

  @weight(3)
  g    (">")  { this.chosenCompOp = ">"; }

  @weight(3)
  le   ("<=") { this.chosenCompOp = "<="; }

  @weight(3)
  ge   (">=") { this.chosenCompOp = ">="; }

}

@copy(schemas_before, inInsertStatement)
class FromClause {

  inh schemas_before : SchemaSet;

  inh usable_columns_outside : UsableColumnList;
  inh inInsertStatement : boolean;

  syn query_results : QueryResultList;

  fr_trl ("FROM ${trl : TableReferenceList}") {
    trl.query_results_before = (QueryResultList:getEmpty);
    trl.usable_columns = this.usable_columns_outside;
    trl.canBeUnique = true;
    this.query_results = trl.query_results_after;
  }

}

@list(3)
@copy(schemas_before, usable_columns, inInsertStatement)
class TableReferenceList {

  inh schemas_before : SchemaSet;

  inh usable_columns : UsableColumnList;

  inh query_results_before : QueryResultList;
  inh inInsertStatement : boolean;

  inh canBeUnique : boolean; # joins of all kinds destroy uniqueness of values!

  syn query_results_after : QueryResultList;

  @weight(10)
  trl ("${tr : TableReference}") {
    tr.query_results_before = this.query_results_before;
    tr.cur_query_res = (QueryResult:getEmpty);
    tr.canBeUnique = this.canBeUnique;
    this.query_results_after =
      (QueryResultList:add this.query_results_before tr.named_query_res_after);
  }

  trll ("${trl : TableReferenceList}, ${tr : TableReference}") {
    trl.query_results_before = this.query_results_before;
    tr.query_results_before = trl.query_results_after;
    tr.cur_query_res = (QueryResult:getEmpty);
    trl.canBeUnique = false;
    tr.canBeUnique = false;
    this.query_results_after =
      (QueryResultList:add trl.query_results_after tr.named_query_res_after);
  }

}

@copy(schemas_before, query_results_before, usable_columns, cur_query_res, inInsertStatement,
  named_query_res_after)
class TableReference {

  inh schemas_before : SchemaSet;

  inh usable_columns : UsableColumnList;

  inh query_results_before : QueryResultList;
  inh inInsertStatement : boolean;

  inh cur_query_res : QueryResult;
  inh canBeUnique : boolean;

  syn named_query_res_after : QueryResult;

  @copy(canBeUnique)
  @weight(5)
  table_factor ("${tf : TableFactor}") { }

  joined_table ("(${jt : JoinedTable})") { }

}

@copy(schemas_before, query_results_before, usable_columns, cur_query_res, inInsertStatement,
  named_query_res_after)
class JoinedTable {

  inh schemas_before : SchemaSet;

  inh query_results_before : QueryResultList;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  inh cur_query_res : QueryResult;
  syn named_query_res_after : QueryResult;

  crossjoin ("${cr : CrossJoin}") { }

  qualifiedjoin ("${qj : QualifiedJoin}") { }

}

@copy(schemas_before, query_results_before, usable_columns, inInsertStatement)
class QualifiedJoin {

  inh schemas_before : SchemaSet;
  inh query_results_before : QueryResultList;

  inh cur_query_res : QueryResult;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  syn named_query_res_after : QueryResult;

  trtrjs ("${tr1 : TableReference} ${ojt : OptionalJoinType} JOIN ${tr2 : TableReferenceIndirection} ${js : JoinSpecification}") {
    # current join listing propagation, for naming purposes!
    tr1.cur_query_res = this.cur_query_res;
    # mergecrossjoin to prevent namecollisions here, take all before and add leftside to it!
    tr2.cur_query_res = (QueryResult:mergeCrossJoin this.cur_query_res tr1.named_query_res_after);

    tr1.canBeUnique = false;
    tr2.canBeUnique = false;

    # the result itself is determined by the joinspecification!
    js.query_res_leftside = tr1.named_query_res_after;
    js.query_res_rightside = tr2.named_query_res_after;

    this.named_query_res_after = js.named_query_res_after;
  }

}

@copy
class TableReferenceIndirection {

  inh schemas_before : SchemaSet;

  inh query_results_before : QueryResultList;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;
  inh canBeUnique : boolean;

  inh cur_query_res : QueryResult;

  syn named_query_res_after : QueryResult;

  tr ("${tr : TableReference}") { }

}

@copy(schemas_before, query_res_leftside, query_res_rightside, inInsertStatement)
class JoinSpecification {

  inh schemas_before : SchemaSet;

  inh query_res_leftside : QueryResult;
  inh query_res_rightside : QueryResult;

  inh inInsertStatement : boolean;

  syn named_query_res_after : QueryResult;

  grd allowed;
  grd implemented;

  jc ("${jc : JoinCondition}") {
    this.allowed= true;
    this.implemented = true;

    this.named_query_res_after =
      (QueryResult:mergeQualifiedJoinJoincondition this.query_res_leftside this.query_res_rightside);
  }

}

@copy(schemas_before, query_res_leftside, query_res_rightside, inInsertStatement)
class JoinCondition {

  inh schemas_before : SchemaSet;
  inh inInsertStatement : boolean;

  inh query_res_leftside : QueryResult;
  inh query_res_rightside : QueryResult;

  @weight(5)
  osc ("ON ${sc : SearchCondition}") {
    # use all columns from both, add them first to resultlist.
    sc.usable_columns =
      (QueryResultList:getColumnsAsUsableForJoinClause
        (QueryResultList:add
          (QueryResultList:add (QueryResultList:getEmpty) this.query_res_leftside)
          this.query_res_rightside));

    sc.schemas_before = this.schemas_before;

    sc.inSelectList = false;
    sc.inWhereOfSDU = false;
    sc.inAggregateFunction = false;
  }

}

class OptionalJoinType {

  none ("") { }

  jt ("${jt : JoinType}") { }

}

class JoinType {

  inner ("INNER") { }

  outer ("${ojt : OuterJoinType} ${ook : OptionalOuterKeyword}") { }

}

class OptionalOuterKeyword {

  outer ("OUTER") { }

}

class OuterJoinType {

  left ("LEFT") { }
  right ("RIGHT") { }

}

@copy(schemas_before, usable_columns, query_results_before, inInsertStatement)
class CrossJoin {

  inh schemas_before : SchemaSet;

  inh query_results_before : QueryResultList;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  inh cur_query_res : QueryResult;
  syn named_query_res_after : QueryResult;

  trtf ("${tr : TableReference} CROSS JOIN ${tf : TableFactor}") {
    # current join listing propagation!
    tr.cur_query_res = this.cur_query_res;
    tr.canBeUnique = false;
    tf.canBeUnique = false;
    tf.cur_query_res = (QueryResult:mergeCrossJoin this.cur_query_res tr.named_query_res_after);
    this.named_query_res_after = (QueryResult:mergeCrossJoin tr.named_query_res_after tf.named_query_res_after);
  }
}

@copy(schemas_before, usable_columns, inInsertStatement)
class TableFactor {

  inh schemas_before : SchemaSet;

  inh usable_columns : UsableColumnList;

  inh query_results_before : QueryResultList;
  inh cur_query_res : QueryResult;
  inh inInsertStatement : boolean;
  inh canBeUnique : boolean;

  syn named_query_res_after : QueryResult;

  grd unique_tableref_name;

  @copy(canBeUnique)
  table_primary ("${tp : TablePrimary}") {
    this.unique_tableref_name =
      (and
        (and
          (not (QueryResultList:containsName this.query_results_before tp.res_name))
          (not (SchemaSet:containsSchemaWithName this.schemas_before tp.res_name)))
        (not (QueryResult:containsName this.cur_query_res tp.res_name)));

    this.named_query_res_after = (QueryResult:setName tp.query_res tp.res_name);
  }

}

@copy(schemas_before)
class TablePrimary {

  syn query_res : QueryResult;
  syn res_name : String;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;
  inh canBeUnique : boolean;

  inh schemas_before : SchemaSet;

  @weight(2)
  toqn_wo_corname ("${toqn : TableOrQueryName}") {
    this.query_res = (QueryResult:fromQueryable toqn.chosen_queryable this.canBeUnique);
    this.res_name = (Queryable:getName toqn.chosen_queryable);
  }

  @weight(2)
  toqn_w_corname ("${toqn : TableOrQueryName} AS ${cn : CorrelationName}") {
    this.query_res = (QueryResult:fromQueryable toqn.chosen_queryable this.canBeUnique);
    this.res_name = cn.name;
  }

  dt_w_corname ("${dt : DerivedTable} ${oask : OptionalASKeywoard} ${cn : CorrelationName}") {
    dt.usable_columns = this.usable_columns;
    dt.inInsertStatement = this.inInsertStatement;
    this.query_res = (QueryResult:fromQueryColumnList dt.selected_query_columns cn.name this.canBeUnique);
    this.res_name = cn.name;
  }

}

class OptionalASKeywoard {

  none ("") { }
  as ("AS") { }

}

@copy(schemas_before, usable_columns, inInsertStatement, selected_query_columns)
class DerivedTable {

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  table_subquery ("${rl : ConstructRandomExpectedSelectList}${ts : TableSubquery}") {
    ts.expected_selectlist = rl.constructed_selectlist;
  }

}

@copy(schemas_before, usable_columns, inInsertStatement)
class ScalarSubquery {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  syn constructedType : Type;

  subquery ("${sq : Subquery}${hi : HiddenIdentifier}") {
    loc result_qc = (QueryColumnList:get sq.selected_query_columns 0);
    sq.expected_selectlist = (ExpectedSelectList:getNew hi.name this.expectedType);
    sq.mustBeScalar = true;
    this.constructedType = (QueryColumn:getType .result_qc);
  }

}

@copy(schemas_before, expected_selectlist, usable_columns, inInsertStatement, selected_query_columns)
class TableSubquery {

  inh expected_selectlist : ExpectedSelectList;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  subquery ("${sq : Subquery}") {
    sq.mustBeScalar = false;
  }

}

@copy
class Subquery {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;

  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  query_expr ("(${qe : QueryExpression})") {
    qe.isOnTopLevel = false;
  }

}

@copy
class QueryExpression {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;
  inh inInsertStatement : boolean;
  inh isOnTopLevel : boolean;

  inh usable_columns : UsableColumnList;
  inh schemas_before : SchemaSet;

  syn selected_query_columns : QueryColumnList;
  syn sort_elem_list : SortElementList;

  syn limit_count : int;

  qeb ("${qeb : QueryExpressionBody} ${oobc : OptionalOrderByClause} ${offc : OptionalFetchFirstOffsetClause}") {
    oobc.selected_query_columns = qeb.selected_query_columns;

    offc.ordered = (not (SortElementList:isEmpty oobc.sort_elem_list));
    offc.isUniquelyOrdered = (SortElementList:isUniqueOrdering oobc.sort_elem_list);
    this.limit_count = offc.limit_count;
  }

}

@copy(schemas_before, expected_selectlist, usable_columns, inInsertStatement)
class QueryExpressionBody {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;
  inh inInsertStatement : boolean;

  inh usable_columns : UsableColumnList;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  grd allowed;

  @weight(3)
  @copy(mustBeScalar, selected_query_columns)
  qt ("${qt : QueryTerm}") {
    this.allowed = true;
  }

  @copy(mustBeScalar)
  union_or_except ("${qeb : QueryExpressionBody} ${uoe : UnionOrExcept} ${oad : OptionalALLDISTINCT} ${qt : QueryTerm}") {
    oad.op = uoe.op;
    # Now from outside given, so everything is 'fine' and we can take anyone we want to.
    # Approximation: union/except both destroy uniqueness
    this.selected_query_columns = (QueryColumnList:removeUniqueQuantifier qt.selected_query_columns);
    this.allowed = (not this.mustBeScalar);
  }

}

class OptionalALLDISTINCT {

  inh op : String;

  grd allowed;

  none ("") {
    this.allowed = true;
  }

  all ("ALL") {
    # In MariaDB, except doesn't allow a keyword afterwards
    this.allowed = (StdLib:ne this.op "except");
  }

  distinct ("DISTINCT") {
    # In MariaDB, except doesn't allow a keyword afterwards
    this.allowed = (StdLib:ne this.op "except");
  }

}

class UnionOrExcept {

  syn op : String;

  union ("UNION") {
    this.op = "union";
  }

  except ("EXCEPT") {
    this.op = "except";
  }

}

class QueryTerm {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;
  inh inInsertStatement : boolean;

  inh usable_columns : UsableColumnList;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  @copy
  @weight(3)
  qp ("${qp : QueryPrimary}") { }

}

class QueryPrimary {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;
  inh inInsertStatement : boolean;

  inh usable_columns : UsableColumnList;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  @copy
  st ("${st : SimpleTable}") { }

}

@copy(schemas_before, expected_selectlist,mustBeScalar, inInsertStatement, selected_query_columns)
class SimpleTable {

  inh mustBeScalar : boolean;

  inh expected_selectlist : ExpectedSelectList;
  inh inInsertStatement : boolean;

  inh usable_columns : UsableColumnList;

  inh schemas_before : SchemaSet;
  syn selected_query_columns : QueryColumnList;

  qs ("${qs : QuerySpecification}") {
    qs.usable_columns_outside = this.usable_columns;
    qs.isOnTopLevel = false;
  }

}

class CorrelationName {

  syn name : String;

  id ("${id :Identifier}") {
    this.name = id.str;
  }

}

@max_alternatives(5)
class TableOrQueryName {

  inh schemas_before : SchemaSet;

  syn chosen_queryable : Queryable;

  grd allowed;

  tablename ("${ptn : PrintTableName}") {
    loc rand_table = (SchemaSet:getRandomTable this.schemas_before this);
    this.chosen_queryable = .rand_table;
    ptn.queryable = .rand_table;

    this.allowed = (SchemaSet:containsTables this.schemas_before);
  }

  queryname ("${ptn : PrintTableName}") {
    loc rand_view = (SchemaSet:getRandomView this.schemas_before this);
    this.chosen_queryable = .rand_view;
    ptn.queryable = .rand_view;

    this.allowed = (SchemaSet:containsViews this.schemas_before);
  }

}

@copy(schemas_before)
@unit
class InsertStatement {

  inh schemas_before : SchemaSet;

  grd allow;

  insert ("INSERT INTO ${ptn : PrintTableName} ${icas : InsertColumnsAndSource}") {
    loc chosen_table = (SchemaSet:getRandomTable this.schemas_before this);

    this.allow = (SchemaSet:containsTables this.schemas_before);

    ptn.queryable = .chosen_table;
    icas.table = .chosen_table;

    icas.inInsertStatement = true;
  }

}

@copy(schemas_before, inInsertStatement, table)
class InsertColumnsAndSource {

  inh schemas_before : SchemaSet;
  inh table : Table;
  inh inInsertStatement : boolean;

  from_constructor ("${oicl : OptionalInsertColumnList} ${ctvc : ContextuallyTypedTableValueConstructor}") {
    ctvc.chosen_tablecolumns = oicl.chosen_tablecolumns;
  }

}

# inserting into a table with only an identity field is not possible, so set an upper bound
# for retrys here
class OptionalInsertColumnList {

  inh table : Table;

  syn chosen_tablecolumns : TableColumns;

  grd allow;

  take_all_implicit ("( ${pr : PrintTableColumns} )") {
    loc ch_tablecol = (Table:getAllTableColumns this.table false);
    this.allow = (not (TableColumns:isEmpty .ch_tablecol));
    this.chosen_tablecolumns = .ch_tablecol;
    pr.tablecolumns = .ch_tablecol;
  }

  take_all_explicit ("") {
    this.allow = (not (Table:hasIdentityColumn this.table));
    this.chosen_tablecolumns = (Table:getAllTableColumns this.table true);
  }

  @weight(3)
  take_some_ordered ("( ${pr : PrintTableColumns} )") {
    loc ch_tablecol = (Table:getRandomColumns this.table false this);

    this.allow = true;

    pr.tablecolumns = .ch_tablecol;
    this.chosen_tablecolumns = .ch_tablecol;
  }

}

class ContextuallyTypedTableValueConstructor {

  inh schemas_before : SchemaSet;
  inh chosen_tablecolumns : TableColumns;
  inh inInsertStatement : boolean;

  @copy
  rowvalues ("VALUES ${ctrvel : ContextuallyTypedRowValueExpressionList}") {
    ctrvel.inSelectList = false;
  }

}

@list
@copy
class ContextuallyTypedRowValueExpressionList {

  inh schemas_before : SchemaSet;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh chosen_tablecolumns : TableColumns;

  first_elem ("${ctrvc : ContextuallyTypedRowValueConstructor}") { }

  rest_elem ("${ctrvel : ContextuallyTypedRowValueExpressionList}, ${ctrvc : ContextuallyTypedRowValueConstructor}") { }

}

@copy(chosen_tablecolumns, schemas_before, inInsertStatement, inSelectList)
class ContextuallyTypedRowValueConstructor {

  inh schemas_before : SchemaSet;
  inh chosen_tablecolumns : TableColumns;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  # technically, this is not allowed by standard, one columns should use ROW, but postgres doesnt support it but this invalid syntax...
  ctrvcsl ("(${ctrvcel : ContextuallyTypedRowValueConstructorElementList})") {
    ctrvcel.cur_index = 0;
    ctrvcel.end_index = (StdLib:sub (TableColumns:size this.chosen_tablecolumns) 1);
  }

}

@copy(schemas_before, inInsertStatement, inSelectList, chosen_tablecolumn)
class ContextuallyTypedRowValueConstructorElement {

  inh schemas_before : SchemaSet;
  inh chosen_tablecolumn : TableColumn;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  grd implemented;

  @weight(10)
  val_exp ("#{(Type:getCastFunction (TableColumn:getType this.chosen_tablecolumn))}(${ve : ValueExpression} ${ad_args : AdditionalCastFunctionArguments})") {
    this.implemented = true;
    ve.expectedType = (Type:getCastFunctionSuperType (TableColumn:getType this.chosen_tablecolumn));
    ve.usable_columns = (UsableColumnList:getEmpty);
    ve.inWhereOfSDU = false;
    ve.inAggregateFunction = false;
  }

}

class AdditionalCastFunctionArguments {

  inh chosen_tablecolumn : TableColumn;

  grd type_allowed;

  chartype_len (", #{(Type:getLength (TableColumn:getType this.chosen_tablecolumn))}") {
    this.type_allowed = (Type:hasLength (TableColumn:getType this.chosen_tablecolumn));
  }

  else ("") {
    this.type_allowed = (not (Type:hasLength (TableColumn:getType this.chosen_tablecolumn)));
  }

}

@copy(schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, usable_columns,
  inSelectList, isUnique, isConstant)
class ValueExpression {

  inh schemas_before : SchemaSet;
  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;

  inh expectedType : Type;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  grd allowed;

  cve ("${cve : CommonValueExpression}") {
    this.allowed = true;
    cve.expectedType = this.expectedType;
    this.constructedType = cve.constructedType;
  }

  bve ("${bve : BooleanValueExpression}") {
    this.allowed = (Type:isBooleanType this.expectedType);
    this.constructedType = (Type:getBOOLEAN);
  }

}

@copy(schemas_before, usable_columns, inWhereOfSDU, inAggregateFunction, inInsertStatement,
  inSelectList, isUnique, isConstant)
class CommonValueExpression {

  inh schemas_before : SchemaSet;
  syn constructedType : Type;
  inh expectedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  grd typecorrect;

  @copy(constructedType)
  nve ("${nve : NumericValueExpression}") {
    this.typecorrect = (Type:isNumericType this.expectedType);
  }

  @copy(expectedType,constructedType)
  rve ("${rve : ReferenceValueExpression}") {
    this.typecorrect = true;
  }

}

class ReferenceValueExpression {

  inh schemas_before : SchemaSet;

  inh expectedType : Type;
  syn constructedType : Type;

  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy
  vep ("${vep : ValueExpressionPrimary}") { }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class NumericValueExpression {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn constructedType : Type;
  syn isUnique : boolean;
  syn isConstant : boolean;

  @weight(5)
  @copy(constructedType, isUnique, isConstant)
  term ("${term : Term}") { }

  nvepmt ("${nve : NumericValueExpression} ${pm : PLUSMINUS} ${term : Term}") {
    this.constructedType = (NumericType:getBiggerType nve.constructedType term.constructedType);
    this.isUnique = (or (and nve.isConstant term.isUnique) (and nve.isUnique term.isConstant));
    this.isConstant = (and nve.isConstant term.isConstant);
  }

}

@copy(usable_columns, schemas_before, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class Term {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn constructedType : Type;
  syn isUnique : boolean;
  syn isConstant : boolean;

  @weight(5)
  @copy(constructedType, isConstant, isUnique)
  factor ("${factor : Factor}") { }

  tmuldivfac ("${term : Term} ${md : MULDIV} ${factor : Factor}") {
    this.constructedType = (NumericType:getBiggerType term.constructedType factor.constructedType);
    this.isConstant = (and term.isConstant factor.isConstant);
    this.isUnique = false; # Problem: 0 * x is not unique!
  }
}

@copy(schemas_before, usable_columns, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList)
class Factor {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn constructedType : Type;
  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy(constructedType, isUnique, isConstant)
  osnp ("${os : OptionalSign}${np : NumericPrimary}") { }

}

class OptionalSign {

  @weight(5)
  none ("") { }

  sign ("${sign : Sign}") { }

}

class Sign {

  pm ("${pm : PLUSMINUS}") { }

}

@copy(schemas_before, usable_columns, inWhereOfSDU, inAggregateFunction, inInsertStatement, inSelectList,
  constructedType)
class NumericPrimary {

  inh schemas_before : SchemaSet;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy(isUnique, isConstant)
  vep ("${vep : ValueExpressionPrimary}") {
    vep.expectedType = (Type:getDECIMAL);
  }

  @copy(isUnique, isConstant)
  nvf ("${nvf : NumericValueFunction}") {
    nvf.expectedType = (Type:getDECIMAL);
  }

}

@copy
class NumericValueFunction {

  inh schemas_before : SchemaSet;

  inh expectedType : Type;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  @copy(isConstant)
  abs ("${abs : AbsoluteValueFunction}") {
    this.isUnique = false; # -x and x are both x, so not unique anymore!
  }

}

@copy
class AbsoluteValueFunction {

  inh schemas_before : SchemaSet;

  inh expectedType : Type;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isConstant : boolean;

  abs ("ABS(${nve : NumericValueExpression})") {
    this.isConstant = nve.isConstant; # if the expression is constant, the absolute value is also constant!
  }

}

@copy
class ValueExpressionPrimary {

  inh schemas_before : SchemaSet;

  inh expectedType : Type;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  pve ("${pve : ParenthesizedValueExpression}") { }

  nvep ("${nvep : NonparenthesizedValueExpressionPrimary}") { }

}

@copy(schemas_before, expectedType, inInsertStatement, inAggregateFunction, inSelectList, constructedType)
class NonparenthesizedValueExpressionPrimary {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;
  inh usable_columns : UsableColumnList;

  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn constructedType : Type;
  syn isUnique : boolean;
  syn isConstant : boolean;

  grd allowed;

  @copy(isUnique, isConstant)
  uvs ("${uvs : UnsignedValueSpecification}") {
    this.allowed = true;
  }

  @weight(10)
  cr ("${cr : ColumnReference}") {
    loc scalarCols = (UsableColumnList:getScalarUsableColumns this.usable_columns);
    loc scalarColsHasMatchingType = (UsableColumnList:hasObjectWithType .scalarCols this.expectedType);
    this.allowed = .scalarColsHasMatchingType;
    cr.usable_columns = .scalarCols;
    this.isConstant = false;
    this.isUnique = cr.isUnique;
  }

  @weight(1)
  sfs ("${sfs : SetFunctionSpecification}") {
    loc groupCols = (UsableColumnList:getGroupedUsableColumns this.usable_columns);
    this.allowed = (not (UsableColumnList:isEmpty .groupCols));
    sfs.usable_columns = (UsableColumnList:addScalarPropertyToAll .groupCols);
    this.isUnique = false;
    this.isConstant = false;
  }

  @weight(3)
  wf ("${wf : WindowFunction}") {
    wf.usable_columns = (UsableColumnList:getScalarUsableColumns this.usable_columns);
    this.allowed = (and this.inSelectList (Type:isNumericType this.expectedType));
    this.isUnique = false;
    this.isConstant = false;
  }

  @weight(4)
  ssq ("${ssq : ScalarSubquery}") {
    ssq.usable_columns = (UsableColumnList:getScalarUsableColumns this.usable_columns);
    # In Inserts, this leads to problems if specified as source and target in MariaDB
    # So don't allow it there!
    this.allowed = (not this.inInsertStatement);
    this.isUnique = false;   # different calls can return same values
    this.isConstant = false; # different calls can return different values
  }

}

class WindowFunction {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh expectedType : Type;
  syn constructedType : Type;

  @copy(schemas_before,usable_columns)
  wftownos ("${wft : WindowFunctionType} OVER ${wnos : WindowNameOrSpecification}") {
    wft.expectedType = this.expectedType;
    this.constructedType = wft.constructedType;

    wnos.winfunc_cat = wft.winfunc_cat;
  }

}

@copy(expectedType, constructedType)
class WindowFunctionType {

  inh schemas_before : SchemaSet;
  inh usable_columns : UsableColumnList;

  inh expectedType : Type;
  syn constructedType : Type;

  syn winfunc_cat : String;

  rft ("${rft : RankedFunctionType}()") {
    this.winfunc_cat = "rank";
  }

  @copy(schemas_before)
  agg ("${agg : AggregateFunction}") {
    this.winfunc_cat = "aggregate";
    agg.usable_columns = (UsableColumnList:getScalarUsableColumns this.usable_columns);
    agg.inAggregateFunction = false;
    agg.inInsertStatement = false;
    agg.asWindowFunction = true;
  }

}

@copy
class RankedFunctionType {

  inh expectedType : Type;
  syn constructedType : Type;

  grd allowed;

  rank ("RANK") {
    this.constructedType = (Type:getINTEGER);
    this.allowed = (Type:isNumericType this.expectedType);
  }

  dense_rank ("DENSE_RANK") {
    this.constructedType = (Type:getBIGINT);
    this.allowed = (Type:isNumericType this.expectedType);
  }

}

@copy
class WindowNameOrSpecification {

  inh winfunc_cat : String;
  inh usable_columns : UsableColumnList;

  ws ("${ws : WindowSpecification}") { }

}

@copy
class WindowSpecification {

  inh winfunc_cat : String;
  inh usable_columns : UsableColumnList;

  wsd ("(${wsd : WindowSpecificationDetails})") { }

}

class WindowSpecificationDetails {

  inh winfunc_cat : String;
  inh usable_columns : UsableColumnList;

  @copy(usable_columns)
  ewnwpcocfc ("${owpc : OptionalWindowPartitionClause} ${owoc : OptionalWindowOrderClause} ${owfc : OptionalWindowFrameClause}") {
    owoc.winfunc_cat = this.winfunc_cat;
    owfc.winfunc_cat = this.winfunc_cat;
    owfc.sort_elem_list = owoc.sort_elem_list;
  }

}

class OptionalWindowFrameClause {

  inh winfunc_cat : String;

  inh sort_elem_list : SortElementList;

  grd allowed;

  nowifrcl ("") {
    this.allowed = true;
  }

  @copy(sort_elem_list)
  wfc ("${wfc : WindowFrameClause}") {
    this.allowed = (not (StdLib:eq this.winfunc_cat "rank"));
  }

}

class WindowFrameClause {

  inh sort_elem_list : SortElementList;

  # Here, some other rules would be possible, but they are not implemented by most databases
  wfu ("${wfu : WindowFrameUnits} ${wfe : WindowFrameExtent}") {
    wfe.uniq_ordering = (SortElementList:isUniqueOrdering this.sort_elem_list);
    wfu.sk_num = (SortElementList:size this.sort_elem_list);
  }

}

class WindowFrameUnits {

  inh sk_num : int;

  grd allowed;

  rows ("ROWS") {
    this.allowed = true;
  }

  range ("RANGE") {
    this.allowed = (StdLib:eq this.sk_num 1);
  }

}

@copy(uniq_ordering)
class WindowFrameExtent {

  inh uniq_ordering : boolean;

  wfs ("${wfs : WindowFrameStart}") {
    wfs.ub_preced_allow = true;
  }

  wfb ("${wfb : WindowFrameBetween}") { }

}

@copy(uniq_ordering)
class WindowFrameBetween {

  inh uniq_ordering : boolean;

  grd allowed;

  wfb ("BETWEEN ${wfb1 : WindowFrameBound1} AND ${wfb2 : WindowFrameBound2}") {
    this.allowed = (and
      (not (and (StdLib:eq wfb1.bound "cur_row") (StdLib:eq wfb2.bound "wf_preceding")))
      (not (and (StdLib:eq wfb1.bound "wf_following") (or
                          (StdLib:eq wfb2.bound "wf_preceding")
                          (StdLib:eq wfb2.bound "cur_row")
                          ))));
  }

}

@copy(uniq_ordering, bound)
class WindowFrameBound1 {

  inh uniq_ordering : boolean;

  syn bound : String;

  wfb ("${wfb : WindowFrameBound}") {
    wfb.ub_preced_allow = true;
    wfb.ub_follow_allow = false;
  }

}

@copy(uniq_ordering, bound)
class WindowFrameBound2 {

  inh uniq_ordering : boolean;

  syn bound : String;

  wfb ("${wfb : WindowFrameBound}") {
    wfb.ub_preced_allow = false;
    wfb.ub_follow_allow = true;
  }

}

@copy(uniq_ordering)
class WindowFrameBound {

  inh ub_follow_allow : boolean;
  inh ub_preced_allow : boolean;
  inh uniq_ordering : boolean;

  syn bound : String;

  grd implemented;
  grd allowed;

  @copy(bound)
  wfs ("${wfs : WindowFrameStart}") {
    this.implemented = true;
    this.allowed = true;
    wfs.ub_preced_allow = this.ub_preced_allow;
  }

  uf ("UNBOUNDED FOLLOWING") {
    this.implemented = this.uniq_ordering;
    this.allowed = this.ub_follow_allow;
    this.bound = "unbounded_following";
  }

  wff ("${wff : WindowFrameFollowing}") {
    this.implemented = this.uniq_ordering;
    this.allowed = true;
    this.bound = "wf_following";
  }

}

class WindowFrameStart {

  inh ub_preced_allow : boolean;
  inh uniq_ordering : boolean;

  syn bound : String;

  grd implemented;
  grd allowed;

  up ("UNBOUNDED PRECEDING") {
    this.implemented = this.uniq_ordering;
    this.allowed = this.ub_preced_allow;
    this.bound = "wf_preceding";
  }

  cr ("CURRENT ROW") {
    this.implemented = true;
    this.allowed = true;
    this.bound = "cur_row";
  }

  wfp ("${wfp : WindowFramePreceding}") {
    this.implemented = this.uniq_ordering;
    this.allowed = true;
    this.bound = "wf_preceding";
  }

}

class WindowFrameFollowing {

  none ("${uvs : UnsignedValueSpecification} FOLLOWING") {
    uvs.expectedType = (Type:getDECIMAL);
  }

}

class WindowFramePreceding {

  none ("${uvs : UnsignedValueSpecification} PRECEDING") {
    uvs.expectedType = (Type:getDECIMAL);
  }

}

class OptionalWindowPartitionClause {

  inh usable_columns : UsableColumnList;
  grd allowed;

  nopartition ("") {
    this.allowed = true;
  }

  @copy(usable_columns)
  wpc ("${wpc : WindowPartitionClause}") {
    this.allowed = true;
  }

}

class WindowPartitionClause {

  inh usable_columns : UsableColumnList;

  @copy(usable_columns)
  wpcrl ("PARTITION BY ${wpcrl : WindowPartitionColumnReferenceList}") { }

}

@list(10)
@copy(usable_columns)
class WindowPartitionColumnReferenceList {

  inh usable_columns : UsableColumnList;

  wpcr ("${wpcr : WindowPartitionColumnReference}") { }

  wpcrlwpcr ("${wpcrl : WindowPartitionColumnReferenceList}, ${wpcr : WindowPartitionColumnReference}") { }

}

@copy(usable_columns)
class WindowPartitionColumnReference {

  inh usable_columns : UsableColumnList;

  cr ("${cr : ColumnReference}") {
    cr.expectedType = (Type:getSUPERTYPE);
  }

}

class OptionalWindowOrderClause {

  inh winfunc_cat : String;
  inh usable_columns : UsableColumnList;
  syn sort_elem_list : SortElementList;

  grd allowed;

  noorder ("") {
    this.allowed = (not (StdLib:eq this.winfunc_cat "rank"));
    this.sort_elem_list = (SortElementList:getEmpty);
  }

  @copy(usable_columns, sort_elem_list)
  woc ("${woc : WindowOrderClause}") {
    this.allowed = (not (UsableColumnList:isEmpty this.usable_columns));
  }

}

class WindowOrderClause {

  inh usable_columns : UsableColumnList;
  syn sort_elem_list : SortElementList;

  @copy(sort_elem_list)
  obssl ("ORDER BY ${ssl : SortSpecificationList}") {
    ssl.schemas_before = (SchemaSet:emptySet); # no schema needed
    ssl.need_real_ordering = true;
    ssl.allow_numbered_ordering = false;
    ssl.selected_query_columns =
      (UsableColumnList:transformToQueryColumnsForWindowOrderByFunction this.usable_columns);
  }

}


@copy
class SetFunctionSpecification {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;
  inh usable_columns : UsableColumnList;

  syn constructedType : Type;

  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;

  grd typematching;

  ag ("${af : AggregateFunction}") {
    af.asWindowFunction = false;
    this.typematching = (Type:isNumericType this.expectedType);
  }

}

class AggregateFunction {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;
  inh usable_columns : UsableColumnList;

  syn constructedType : Type;

  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh asWindowFunction : boolean;

  grd allowed;

  @copy
  gsf ("${gsf : GeneralSetFunction}") {
    this.allowed = (not this.inAggregateFunction);
  }

}

class GeneralSetFunction {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;
  inh usable_columns : UsableColumnList;
  inh inInsertStatement : boolean;
  inh asWindowFunction : boolean;

  syn constructedType : Type;

  @copy(schemas_before, usable_columns, inInsertStatement,)
  gsf ("${sft : SetFunctionType}(${osq : OptionalSetQuantifier} ${ve : ValueExpression}${hdt : HiddenDataType})") {
    loc argumentType = hdt.randomSuperType;
    sft.expectedType = this.expectedType;
    sft.argumentType = .argumentType;

    ve.inWhereOfSDU = false;
    ve.expectedType = .argumentType;

    ve.inAggregateFunction = true;
    ve.inSelectList = false;

    osq.allowDistinct = (not this.asWindowFunction);

    this.constructedType = sft.constructedType;
  }

}

# see also https://www.postgresql.org/docs/11/functions-aggregate.html
class SetFunctionType {

  inh expectedType : Type;
  inh argumentType : Type;

  syn constructedType : Type;

  grd type_correct_argument;
  grd type_correct_expected;

  cnt ("COUNT") {
    this.type_correct_expected = (Type:isNumericType this.expectedType);
    this.type_correct_argument = true;
    this.constructedType = (Type:getBIGINT);
  }

  max ("MAX") {
    this.type_correct_expected = (Type:isNumericType this.expectedType);
    this.type_correct_argument = (Type:isImplicitConvertible this.argumentType this.expectedType);
    this.constructedType = this.expectedType;
  }

  min ("MIN") {
    this.type_correct_expected = (Type:isNumericType this.expectedType);
    this.type_correct_argument = (Type:isImplicitConvertible this.argumentType this.expectedType);
    this.constructedType = this.expectedType;
  }

  sum ("SUM") {
    this.type_correct_expected = (Type:isNumericType this.expectedType);
    this.type_correct_argument = (Type:isImplicitConvertible this.argumentType this.expectedType);
    this.constructedType = (Type:getBIGINT); #Not completely correct, only for supported types
  }

}

@copy
class ParenthesizedValueExpression {

  inh schemas_before : SchemaSet;
  inh expectedType : Type;
  syn constructedType : Type;
  inh usable_columns : UsableColumnList;

  inh inWhereOfSDU : boolean;
  inh inAggregateFunction : boolean;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  syn isUnique : boolean;
  syn isConstant : boolean;

  lpverp ("(${ve : ValueExpression})") { }

}

class UnsignedValueSpecification {

  inh expectedType : Type;
  syn constructedType : Type;
  syn isUnique : boolean;
  syn isConstant : boolean;

  ul ("${ul : UnsignedLiteral}") {
    ul.expectedType = this.expectedType;
    this.constructedType = ul.constructedType;
    this.isUnique = false;
    this.isConstant = true;
  }

}

@copy(constructedType)
class UnsignedLiteral {

  inh expectedType : Type;
  syn constructedType : Type;

  grd typecorrect;

  unl ("${unl : UnsignedNumericLiteral}") {
    this.typecorrect = (Type:isNumericType this.expectedType);
  }

  gl ("${gl : GeneralLiteral}") {
    this.typecorrect = (Type:isBooleanType this.expectedType);
    gl.expectedType = this.expectedType;
  }

}

class GeneralLiteral {

  inh expectedType : Type;
  syn constructedType : Type;

  grd typecorrect;

  bl ("${bl : BooleanLiteral}") {
    this.typecorrect = (Type:isBooleanType this.expectedType);
    this.constructedType = (Type:getBOOLEAN);
  }

}

class BooleanLiteral {

  t ("TRUE") { }

  f ("FALSE") { }

}

class UnsignedNumericLiteral {

  syn constructedType : Type;

  @copy
  enl ("${enl : ExactNumericLiteral}") { }

}

class ExactNumericLiteral {

  syn constructedType : Type;

  @weight(3)
  smallint_casted ("${si : SMALLUNSIGNEDINTEGER}") {
    this.constructedType = (Type:getINTEGER);
  }

  @weight(3)
  std_int ("${ui : UNSIGNEDINTEGER}") {
    this.constructedType = (Type:getINTEGER);
  }

  big_int ("${bi : BIGUNSIGNEDINTEGER}") {
    this.constructedType = (Type:getBIGINT);
  }

}

class PLUSMINUS("+|-");

# Division leads to floating point, not supported by grammar, add later if needed!
class MULDIV("*"); # ("*|/");

@list
@copy(schemas_before, inInsertStatement, inSelectList)
class ContextuallyTypedRowValueConstructorElementList {

  inh schemas_before : SchemaSet;
  inh inInsertStatement : boolean;
  inh inSelectList : boolean;

  inh chosen_tablecolumns : TableColumns;
  inh cur_index : int;
  inh end_index : int;

  grd endOfList;

  first_elem ("${ctrvce : ContextuallyTypedRowValueConstructorElement}") {
    this.endOfList = (StdLib:eq this.cur_index this.end_index);
    ctrvce.chosen_tablecolumn = (TableColumns:get this.chosen_tablecolumns this.cur_index);
  }

  rest_elem ("${ctrvce : ContextuallyTypedRowValueConstructorElement}, ${ctrvcel : ContextuallyTypedRowValueConstructorElementList}") {
    this.endOfList = (StdLib:ne this.cur_index this.end_index);

    ctrvce.chosen_tablecolumn = (TableColumns:get this.chosen_tablecolumns this.cur_index);

    ctrvcel.chosen_tablecolumns = this.chosen_tablecolumns;
    ctrvcel.cur_index = (StdLib:add this.cur_index 1);
    ctrvcel.end_index = this.end_index;
    ctrvcel.schemas_before = this.schemas_before;
  }

}

class PrintTableColumns {

  inh tablecolumns : TableColumns;

  print ("#{(TableColumns:printCommaSeperated this.tablecolumns)}") { }

}

class PrintTableName {

  inh queryable : Queryable;

  full_qualified ("${ p : PrintString}.${p2 : PrintString}") {
    p.str = (Queryable:getSchemaName this.queryable);
    p2.str = (Queryable:getName this.queryable);
  }

}

class SchemaNameClause {

  inh schemas_before: SchemaSet;
  syn name : String;

  grd name_allowed;

  schema_name ("${schema_name : SchemaName}") {
    this.name = schema_name.name;
    this.name_allowed = (not (SchemaSet:contains this.schemas_before schema_name.name));
  }

}

class PrintString {

  inh str : String;

  print ("#{this.str}") { }

}

@unit
class TableDefinition {

  inh allowed_schemas : SchemaSet;
  syn newtable : Table;

  grd allowed;

  create_table ("CREATE TABLE ${tn : TableName} ${tcs : TableContentsSource}") {
    loc chosen_schema_name = (SchemaSet:getRandomSchemaName this.allowed_schemas this);
    loc chosen_schema = (SchemaSet:getSchemaByName this.allowed_schemas .chosen_schema_name);

    this.allowed = (not (SchemaSet:isEmpty this.allowed_schemas));

    tn.schema = .chosen_schema;
    this.newtable = (Table:getNew tn.table_name tcs.tc (Schema:getSchemaName .chosen_schema));
  }

}

class TableContentsSource {

  syn tc: TableColumns;

  tel ("${tel : TableElementList}") {
    this.tc = tel.tc;
  }

}

class TableElementList {

  syn tc: TableColumns;

  grd hasInsertable;

  telh (" ( ${telh : TableElementListHelper} ) ") {
    telh.tc_before = (TableColumns:getNew);
    this.tc = telh.tc_after;
    this.hasInsertable = (TableColumns:hasInsertable telh.tc_after);
  }

}

@list(10)
class TableElementListHelper {

  inh tc_before : TableColumns;
  syn tc_after : TableColumns;

  two_te ("${te : TableElement}") {
    te.tc_before = this.tc_before;
    this.tc_after = (TableColumns:add this.tc_before te.tc);
  }

  @weight(1)
  multiple_te ("${telhs : TableElementListHelper}, ${te : TableElement}") {
    telhs.tc_before = this.tc_before;
    te.tc_before = telhs.tc_after;
    this.tc_after = (TableColumns:add telhs.tc_after te.tc);
  }

}

class TableElement {

  inh tc_before : TableColumns;

  syn tc : TableColumn;

  cd ("${cd : ColumnDefinition}") {
    cd.tc_before = this.tc_before;
    this.tc = cd.cd;
  }

}

class ColumnDefinition {

  inh tc_before : TableColumns;

  syn cd : TableColumn;

  cd ("${cn : ColumnName} ${dtodn : DataTypeOrDomainName} ${odcoics : OptionalDefClauseOrIdentColSpec}") {
    cn.tc_before = this.tc_before;
    odcoics.coltype = dtodn.type;
    odcoics.tc_before = this.tc_before;
    this.cd = (TableColumn:getNew cn.name dtodn.type odcoics.isIdentityCol);
  }

}

class OptionalDefClauseOrIdentColSpec {

  inh tc_before : TableColumns;
  inh coltype : Type;

  syn isIdentityCol : boolean;

  none ("") {
    this.isIdentityCol = false;
  }

  @copy
  dcoics ("${dcoics : DefClauseOrIdentColSpec}") { }

}


class DefClauseOrIdentColSpec {

  inh tc_before : TableColumns;
  inh coltype : Type;

  syn isIdentityCol : boolean;

  grd typeallowed;
  grd onlyoneident;

  @copy(coltype)
  dc ("${dc : DefaultClause}") {
    this.isIdentityCol = false;
    this.typeallowed = true;
    this.onlyoneident = true;
  }

  @weight(3)
  ics ("${ics : IdentityColumnSpecification}") {
    this.isIdentityCol = true;
    this.onlyoneident = (not (TableColumns:hasUnique this.tc_before));
    this.typeallowed = (Type:isNumericType this.coltype);
  }

}


class DefaultClause {

  inh coltype : Type;

  @copy(coltype)
  dc ("DEFAULT ${dl : DefaultLiteral}") { }

}

class DefaultLiteral {

  inh coltype : Type;

  grd typeallowed;

  int ("${sm : SMALLINT}") {
    this.typeallowed = (Type:isNumericType this.coltype);
  }

  bool ("${bl : BooleanLiteral}") {
    this.typeallowed = (Type:isBooleanType this.coltype);
  }

}

class IdentityColumnSpecification {

  # technically, not correct, but each database has own syntax so we need an easy replace mchanism)
  id ("GENERATED BY DEFAULT AS IDENTITY") { }

}

class DataTypeOrDomainName {

  syn type : Type;

  data_type ("${dat : DataType}") {
    this.type = dat.type;
  }

}

@hidden
class HiddenDataType {

  syn randomSuperType : Type;

  dt ("${dt : DataType}") {
    this.randomSuperType = (Type:getTypeclass dt.type);
  }

}

class DataType {

  syn type : Type;

  num_type ("${nt : NumericType}") {
    this.type = nt.type;
  }

  bool_type ("${bt : BooleanType}") {
    this.type = bt.type;
  }

}

class BooleanType {

  syn type: Type;

  bool ("BOOLEAN") {
    this.type = (Type:getBOOLEAN);
  }

}

@copy
class NumericType {

  syn type: Type;

  smallint ("${pt : PrintType}") {
    loc chosen_type = (Type:getSMALLINT);
    this.type = .chosen_type;
    pt.type = .chosen_type;
  }

  @weight(3)
  integer ("${pt : PrintType}") {
    loc chosen_type = (Type:getINTEGER);
    this.type = .chosen_type;
    pt.type = .chosen_type;
  }

  @weight(3)
  bigint ("${pt : PrintType}") {
    loc chosen_type = (Type:getBIGINT);
    this.type = .chosen_type;
    pt.type = .chosen_type;
  }

}

class PrintType {

  inh type: Type;

  printtype ("#{(Type:getName this.type)}") { }

}

class ColumnName {

  inh tc_before : TableColumns;

  syn name : String;

  grd name_unique;

  id ("${id : Identifier}") {
    this.name_unique = (not (TableColumns:contains this.tc_before id.str));
    this.name = id.str;
  }

}

class TableName {

  inh schema : Schema;

  syn table_name : String;

  grd unique_table_name;

  losqn ("${p:PrintString}.${qi : QualifiedIdentifier}") {
    this.unique_table_name = (not (Schema:containsQueryableWithName this.schema qi.name));
    this.table_name = qi.name;
    p.str = (Schema:getSchemaName this.schema);
  }

}

class QualifiedIdentifier {

  syn name : String;

  id ("${id: Identifier}") {
    this.name = id.str;
  }

}

@unit
class SchemaDefinition {

  inh schemas_before: SchemaSet;
  syn schemas_after: SchemaSet;

  @copy(schemas_before)
  schema_name_clause ("CREATE SCHEMA ${schema_name_clause : SchemaNameClause}") {
    loc newSchema = (Schema:getNew schema_name_clause.name);
    this.schemas_after = (SchemaSet:add this.schemas_before .newSchema);
  }

}

class SchemaName {

  syn name : String;

  id ("${id : Identifier}") {
    this.name = id.str;
  }

}

@hidden
class HiddenIdentifier {

  syn name : String;

  @copy
  id ("${id : Identifier}") {
    this.name = id.str;
  }

}

# No SQL Keyword starts with Q, so it is a safe prefix to use
# This way, no keyword is accidentally used as an identifier
@count(250)
class Identifier("Q[A-Z]{2,5}");

class SMALLUNSIGNEDINTEGER("0|([1-9][0-9]{0,2})"); #small enough for int.
class UNSIGNEDINTEGER("0|([1-9][0-9]{0,2})"); #small enough for int.
class BIGUNSIGNEDINTEGER("0|([1-9][0-9]{0,2})"); #small enough for int.

class SMALLINT("0|[1-3]");
class NORMALINT("0|[1-3]");
class BIGINT("0|[1-3]");
