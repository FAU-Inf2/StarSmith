use BitVector;
use Symbol;
use SymbolTable;
use Type;

class Script {

  script (
      "(set-logic QF_UFSLIA)\n
      ${cmds : Commands}\n
      (check-sat)\n") {
    cmds.stack_depth_before = 0;
    cmds.symbols_before = (SymbolTable:empty);
  }

}

@list(2000)
@copy(stack_depth_before, symbols_before, stack_depth_after, symbols_after)
class Commands {

  inh stack_depth_before : int; 
  syn stack_depth_after : int; 

  inh symbols_before : SymbolTable;
  syn symbols_after : SymbolTable;

  @weight(1)
  one_cmd ("${cmd : Command}") {
    # intentionally left blank
  }

  @weight(200)
  mult_cmd ("${cmd : Command}\n${rest : Commands}") {
    rest.stack_depth_before = cmd.stack_depth_after;
    this.stack_depth_after = rest.stack_depth_after;

    rest.symbols_before = cmd.symbols_after;
    this.symbols_after = rest.symbols_after;
  }

}

class PushPopInt("[0-9]");

@copy(symbols_before)
class Command {

  inh stack_depth_before : int; 
  syn stack_depth_after : int; 

  inh symbols_before : SymbolTable;
  syn symbols_after : SymbolTable;

  grd allowed;

  @weight(20)
  fun ("${fun : FunDecl}") {
    this.allowed = true;
    this.stack_depth_after = this.stack_depth_before;
    this.symbols_after = fun.symbols_after;
  }

  @weight(8)
  assert ("${assert : Assert}") {
    this.allowed = true;
    this.stack_depth_after = this.stack_depth_before;
    this.symbols_after = this.symbols_before;
  }

  @weight(1)
  push ("(push ${cnt : PushPopInt})") {
    this.allowed = true;
    this.stack_depth_after = (+ this.stack_depth_before (int cnt.str));

    this.symbols_after = (SymbolTable:enterScopes this.symbols_before (int cnt.str));
  }

  @weight(3)
  pop ("(pop ${cnt : PushPopInt})") {
    this.allowed = (<= (int cnt.str) this.stack_depth_before);
    this.stack_depth_after = (- this.stack_depth_before (int cnt.str));

    this.symbols_after = (SymbolTable:leaveScopes this.symbols_before (int cnt.str));
  }

  @weight(2)
  check_sat ("(check-sat)") {
    this.allowed = true;
    this.stack_depth_after = this.stack_depth_before;

    this.symbols_after = this.symbols_before;
  }

}

class Assert {

  inh symbols_before : SymbolTable;

  @weight(2)
  @copy(symbols_before)
  assert ("(assert\+${expr : Expression}\-)") {
    expr.expected_type = (Type:boolSort);
  }

}

@copy(symbols_before)
class FunDecl {

  inh symbols_before : SymbolTable;

  syn symbols_after : SymbolTable;

  grd possible;

  @weight(10)
  # TODO should we include uninterpreted functions?
  declare_fun
      ("(declare-fun ${def : DefIdentifier} ${params : OptSortList} ${ret : Sort})") {
    this.possible = true;
    this.symbols_after =
      (SymbolTable:put
        this.symbols_before
        (Symbol:create def.name (Type:createFunctionType ret.type params.type)));
  }

  @weight(3)
  define_fun
      ("(define-fun ${def : DefIdentifier} ${params : OptParamList} ${ret : Sort}\+
          ${expr : Expression}\-
        )") {
    this.possible =
      (or
        (Type:isPrimitiveSort ret.type)
        (SymbolTable:containsVisibleSymbolOfType this.symbols_before ret.type)
      );

    params.function_symbol = (Symbol:create def.name (Type:unknownSort));
    params.symbols_before = (SymbolTable:empty);

    expr.symbols_before = params.symbols_after;
    expr.expected_type = ret.type;

    this.symbols_after =
      (SymbolTable:put
        this.symbols_before
        (Symbol:create def.name (Type:createFunctionType ret.type params.type)));
  }

}

class OptSortList {

  syn type : Type;

  no_sort_list ("()") {
    this.type = (Type:createEmptyTupleType);
  }

  sort_list ("(${sort_list : SortList})") {
    this.type = sort_list.type;
  }

}

@list
class SortList {

  syn type : Type;

  one_sort ("${sort : Sort}") {
    this.type = (Type:createTupleType sort.type);
  }

  mult_sort ("${sort : Sort} ${rest : SortList}") {
    this.type = (Type:mergeTupleTypes (Type:createTupleType sort.type) rest.type);
  }

}

class OptParamList {

  inh function_symbol : Symbol;

  inh symbols_before : SymbolTable;
  syn symbols_after : SymbolTable;

  syn type : Type;

  no_param_list ("()") {
    this.symbols_after = this.symbols_before;
    this.type = (Type:createEmptyTupleType);
  }

  @copy(function_symbol, symbols_before, symbols_after, type)
  param_list ("(${param_list : ParamList})") {
    # intentionally left blank
  }

}

@list
@copy(function_symbol, symbols_before)
class ParamList {

  inh function_symbol : Symbol;

  inh symbols_before : SymbolTable;
  syn symbols_after : SymbolTable;

  syn type : Type;

  one_param ("${param : Param}") {
    this.symbols_after = (SymbolTable:put this.symbols_before param.symbol);

    this.type = (Type:createTupleType (Symbol:getType param.symbol));
  }

  mult_param ("${param : Param} ${rest : ParamList}") {
    rest.symbols_before = (SymbolTable:put this.symbols_before param.symbol);
    this.symbols_after = rest.symbols_after;

    this.type =
      (Type:mergeTupleTypes (Type:createTupleType (Symbol:getType param.symbol)) rest.type);
  }

}

class Param {

  inh function_symbol : Symbol;
  inh symbols_before : SymbolTable;

  syn symbol : Symbol;

  binding ("(${id : DefIdentifier} ${sort : Sort})") {
    id.symbols_before = (SymbolTable:put this.symbols_before this.function_symbol);
    this.symbol = (Symbol:create id.name sort.type);
  }

}

@count(2000)
class Identifier("[a-z][a-zA-Z0-9_]{2,6}");

class DefIdentifier {

  inh symbols_before : SymbolTable;

  syn name : String;

  grd name_unique;

  def_id ("${id : Identifier}") {
    this.name = id.str;
    this.name_unique = (not (SymbolTable:contains this.symbols_before id.str));
  }

}

class UseIdentifier {

  inh symbols_before : SymbolTable;

  syn symbol : Symbol;

  use_id (SymbolTable:visibleSymbols this.symbols_before (Type:anySort)) : Symbol {
    this.symbol = $;
  }

}

class Sort {

  syn type : Type;

  @weight(2)
  bool ("Bool") {
    this.type = (Type:boolSort);
  }

  @weight(2)
  int ("Int") {
    this.type = (Type:intSort);
  }

  @weight(3)
  string ("String") {
    this.type = (Type:stringSort);
  }

  @feature("regex")
  regLan ("RegLan") {
    this.type = (Type:regLanSort);
  }

}

@copy(symbols_before, expected_type)
class Expression {
  
  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  grd type_matches;

  _true ("true") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);
    this.type = (Type:boolSort);
  }

  _false ("false") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);
    this.type = (Type:boolSort);
  }

  @weight(3)
  var ("${id : UseIdentifier}") {
    this.type_matches = (Type:assignable (Symbol:getType id.symbol) this.expected_type);
    this.type = (Symbol:getType id.symbol);
  }

  @weight(20)
  fun_app ("${fun_app : FunApp}") {
    this.type_matches = true; # checked in FunApp
    this.type = fun_app.type;
  }

  @weight(13)
  @copy(type)
  core_expr ("${expr : Core_Expression}") {
    this.type_matches = (Type:isPrimitiveSort this.expected_type);
  }

  @weight(13)
  @copy(type)
  let ("${let : LetExpression}") {
    this.type_matches = true;
  }

  @weight(50)
  @copy(type)
  string_expr ("${expr : StringExpression}") {
    this.type_matches = true; # checked in StringExpression
  }

  @weight(3)
  @feature("regex")
  @copy(type)
  regex_expr ("${expr : RegExExpression}") {
    this.type_matches = true; # checked in RegExExpression
  }

}

@copy(symbols_before, expected_type)
class FunApp {
  
  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  fun_app_nullary ("${callee : Callee}") {
    callee.nullary = true;
    this.type = (Type:getReturnType (Symbol:getType callee.symbol));
  }

  fun_app_non_nullary ("(${callee : Callee} ${args : ArgumentList})") {
    callee.nullary = false;
    args.expected_type = (Type:getParameterType (Symbol:getType callee.symbol));
    this.type = (Type:getReturnType (Symbol:getType callee.symbol));
  }

}

class Callee {

  inh symbols_before : SymbolTable;
  inh nullary : boolean;
  inh expected_type : Type;

  syn symbol : Symbol;

  callee (SymbolTable:visibleFunctions this.symbols_before this.nullary this.expected_type) : Symbol {
    this.symbol = $;
  }

}

class LetExpression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  grd possible;

  @copy(expected_type)
  let ("(let (${bindings : BindingList}) ${expr : Expression})") {
    this.possible =
      (or
        (Type:isPrimitiveSort this.expected_type)
        (SymbolTable:containsVisibleSymbolOfType this.symbols_before this.expected_type)
      );

    bindings.symbols_before_outer = this.symbols_before;
    bindings.symbols_before_inner = (SymbolTable:enterScope this.symbols_before);

    expr.symbols_before = bindings.symbols_after;

    this.type = expr.type;
  }

}

class LexOrderOperator("<|<=");
class ContainsOperator("prefixof|suffixof|contains");
# 'replace_all' not supported by (older versions of) CVC4
#class ReplaceOperator("replace|replace_all");
class ReplaceOperator("replace");

@copy(symbols_before)
class StringExpression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd type_matches;

  syn type : Type;

  @weight(5)
  string_literal ("${lit : StringLiteral}") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;
  }

  @weight(5)
  concat ("(str.++ ${s : Expression} ${ss : ExpressionChain})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
    ss.min_length = 1;
    ss.expected_type = (Type:stringSort);
  }

  @weight(5)
  len ("(str.len ${expr : Expression})") {
    loc result_type = (Type:intSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    expr.expected_type = (Type:stringSort);
  }

  # 'str.<[=]' is 'chainable' but this is not supported by (some older versions of) CVC4
  @weight(5)
  @feature("str_lex")
  lex_order ("(str.${op : LexOrderOperator} ${one : Expression} ${two : Expression})") {
    loc result_type = (Type:boolSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    one.expected_type = (Type:stringSort);
    two.expected_type = (Type:stringSort);
  }

  @weight(5)
  at ("(str.at ${s : Expression} ${i : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
    i.expected_type = (Type:intSort);
  }

  @weight(5)
  substr ("(str.substr ${s : Expression} ${i1 : Expression} ${i2 : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
    i1.expected_type = (Type:intSort);
    i2.expected_type = (Type:intSort);
  }

  @weight(5)
  contains ("(str.${op : ContainsOperator} ${s1 : Expression} ${s2 : Expression})") {
    loc result_type = (Type:boolSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s1.expected_type = (Type:stringSort);
    s2.expected_type = (Type:stringSort);
  }

  @weight(5)
  indexof ("(str.indexof ${s1 : Expression} ${s2 : Expression} ${i : Expression})") {
    loc result_type = (Type:intSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s1.expected_type = (Type:stringSort);
    s2.expected_type = (Type:stringSort);
    i.expected_type = (Type:intSort);
  }

  @weight(5)
  replace ("(str.${op : ReplaceOperator} ${s1 : Expression} ${s2 : Expression} ${s3 : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s1.expected_type = (Type:stringSort);
    s2.expected_type = (Type:stringSort);
    s3.expected_type = (Type:stringSort);
  }

  @weight(2)
  is_digit ("(str.is_digit ${s : Expression})") {
    loc result_type = (Type:boolSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
  }

  # TODO restrict to singleton strings?
  @weight(1)
  to_code ("(str.to_code ${s : Expression})") {
    loc result_type = (Type:intSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
  }

  @weight(5)
  from_code ("(str.from_code ${i : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    i.expected_type = (Type:intSort);
  }

  @weight(1)
  to_int ("(str.to_int ${s : Expression})") {
    loc result_type = (Type:intSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
  }

  @weight(5)
  from_int ("(str.from_int ${i : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    i.expected_type = (Type:intSort);
  }

}

@count(10000)
class CharacterSeq("[a-zA-Z0-9+-=_{};]{1,100}");

class StringLiteral {

  empty_string ("\"\"") {}

  @weight(10)
  char_seq ("\"${seq : CharacterSeq}\"") {}

  @weight(10)
  singleton_str ("${singleton : SingletonStringLiteral}") {}

}

@count(10000)
class HexNumber("#x[0-9a-fA-F]{1,4}");

@count(100)
class SingletonCharacterSeq("[a-zA-Z0-9+-=_{};]");

class SingletonStringLiteral {

  @weight(1)
  @feature("char")
  char ("(_ char ${h : HexNumber})") {}

  # TODO does this count as a singleton string?
  @weight(1)
  singleton_seq ("\"${seq : SingletonCharacterSeq}\"") {}

}

class BaseRegExOperator("none|all|allchar");
class UnionInterOperator("union|inter");
class KleeneOperator("*|+|opt");
class RegExReplaceOperator("replace_re|replace_re_all");
class CompDiffOperator("comp|diff");
class Index("0|[1-9][0-9]{0,1}");

@copy(symbols_before)
class RegExExpression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd type_matches;

  syn type : Type;

  in_re ("(str.in_re ${s : Expression} ${r : Expression})") {
    loc result_type = (Type:boolSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s.expected_type = (Type:stringSort);
    r.expected_type = (Type:regLanSort);
  }

  base ("(re.${op : BaseRegExOperator})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;
  }

  concat ("(re.++ ${r : Expression} ${rest : ExpressionChain})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
    rest.min_length = 1;
    rest.expected_type = (Type:regLanSort);
  }

  union_inter ("(re.${op : UnionInterOperator} ${r : Expression} ${rest : ExpressionChain})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
    rest.min_length = 1;
    rest.expected_type = (Type:regLanSort);
  }

  kleene ("(re.${op : KleeneOperator} ${r : Expression})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
  }

  replace ("(str.${op : RegExReplaceOperator} ${s1 : Expression} ${r : Expression} ${s2 : Expression})") {
    loc result_type = (Type:stringSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    s1.expected_type = (Type:stringSort);
    r.expected_type = (Type:regLanSort);
    s2.expected_type = (Type:stringSort);
  }

  comp_diff ("(re.${op : CompDiffOperator} ${r : Expression})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
  }

  range ("(re.range ${s1 : SingletonStringLiteral} ${s2 : SingletonStringLiteral})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;
  }

  power ("((re.^ ${i : Index}) ${r : Expression})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
  }

  loop ("((re.loop ${i1 : Index} ${i2 : Index}) ${r : Expression})") {
    loc result_type = (Type:regLanSort);

    this.type_matches = (Type:assignable .result_type this.expected_type);
    this.type = .result_type;

    r.expected_type = (Type:regLanSort);
  }

}

class BindingList {

  inh symbols_before_outer : SymbolTable;
  inh symbols_before_inner : SymbolTable;
  syn symbols_after : SymbolTable;

  one_binding ("${binding : Binding}") {
    binding.symbols_before = this.symbols_before_outer;
    this.symbols_after = (SymbolTable:put this.symbols_before_inner binding.symbol);
  }

  @copy(symbols_before_outer, symbols_after)
  mult_binding ("${binding : Binding} ${rest : BindingList}") {
    binding.symbols_before = this.symbols_before_outer;
    rest.symbols_before_inner = (SymbolTable:put this.symbols_before_inner binding.symbol);
  }

}

class Binding {

  inh symbols_before : SymbolTable;
  syn symbol : Symbol;

  @copy(symbols_before)
  binding ("(${id : DefIdentifier} ${e : Expression})") {
    e.expected_type = (Type:anySort);

    this.symbol = (Symbol:create id.name e.type);
  }

}

class BoolOperator("and|or|xor|=>");

@copy(symbols_before)
class Core_Expression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd type_matches;

  syn type : Type;

  @weight(9)
  not ("(not ${expr : Expression})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    expr.expected_type = (Type:boolSort);

    this.type = (Type:boolSort);
  }

  @weight(13)
  bool_op ("(${op : BoolOperator} ${es : ExpressionChain})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    es.expected_type = (Type:boolSort);
    es.min_length = 2;

    this.type = (Type:boolSort);
  }

  @weight(3)
  ite ("(ite ${c : Expression} ${t : IteHelper} ${e : Expression})") {
    this.type_matches =
      (or
        (Type:isPrimitiveSort this.expected_type)
        (SymbolTable:containsVisibleSymbolOfType this.symbols_before this.expected_type)
      );

    c.expected_type = (Type:boolSort);
    t.expected_type = this.expected_type;
    e.expected_type = t.type;

    this.type = t.type;
  }

  @weight(1)
  eq ("(= ${f : Expression} ${r : ExpressionChain})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    f.expected_type = (Type:anySort);
    r.expected_type = f.type;
    r.min_length = 1;

    this.type = (Type:boolSort);
  }

  @weight(1)
  distinct ("(distinct ${f : Expression} ${r : ExpressionChain})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    f.expected_type = (Type:anySort);
    r.expected_type = f.type;
    r.min_length = 1;

    this.type = (Type:boolSort);
  }

}

class IteHelper {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  @copy
  ite_helper ("${t : Expression}") {
    # intentionally left blank
  }

}

@list
@copy(symbols_before, expected_type)
class ExpressionChain {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh min_length : int;

  grd valid;

  one_expr ("${e : Expression}") {
    this.valid = (<= this.min_length 1);
  }

  mult_expr ("${e : Expression} ${r : ExpressionChain}") {
    this.valid = true;
    r.min_length = (- this.min_length 1);
  }

}

@list
@copy(symbols_before)
class ArgumentList {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd valid;

  one_arg ("${val : Expression}") {
    val.expected_type = (Type:getTupleTypeHead this.expected_type);

    this.valid = (equals (Type:getTupleTypeSize this.expected_type) 1);
  }

  mult_arg ("${val : Expression} ${rest : ArgumentList}") {
    val.expected_type = (Type:getTupleTypeHead this.expected_type);
    rest.expected_type = (Type:getTupleTypeTail this.expected_type);

    this.valid = (> (Type:getTupleTypeSize this.expected_type) 1);
  }

}
