use LuaUtils;
use Set;
use Symbol;
use SymbolTable;
use TableField;
use Tuple;
use Type;

class Script {

  # statements are contained in a one-iteration loop to increase the chance that LuaJIT compiles
  # the code natively...
  script ("
      while 1 == 1 do\+
        ${stmts : OptStmtList}
        break\-
      end\n
      #{(LuaUtils:print stmts.symbols_after)}") {
    stmts.in_global_block = true;
    stmts.in_loop = false;
    stmts.symbols_before = (SymbolTable:empty);
    stmts.expected_return_type = nil; # not inside a function
  }

}

class Block {

  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  @copy(in_loop, symbols_before, expected_return_type)
  block (
      "#{(LuaUtils:printRandomInt this)}
      ${stmts : OptStmtList}
      ${last : OptLastStatement}") {
    loc stmts_break = (not (isNil stmts.symbols_break));

    stmts.in_global_block = false;

    last.symbols_before = stmts.symbols_after;

    this.symbols_after = (SymbolTable:leaveScope stmts.symbols_after);
    this.symbols_break =
      (if .stmts_break
        (SymbolTable:leaveScope stmts.symbols_break)
        (if last.breaks (SymbolTable:leaveScope stmts.symbols_after) nil));
  }

}

@copy
@unit
class OptStmtList {

  inh in_global_block : boolean;
  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  no_stmts ("") {
    this.symbols_after = this.symbols_before;
    this.symbols_break = nil;
  }

  @weight(20)
  stmts ("${stmts : StmtList}\n") {
    # intentionally left blank
  }

}

@copy(in_global_block, in_loop, expected_return_type, symbols_before)
@list(40)
class StmtList {

  inh in_global_block : boolean;
  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  @weight(1)
  @copy(symbols_after, symbols_break)
  one_stmt ("${stmt : Stmt}") {
    # intentionally left blank
  }

  # NOTE: ';' is actually the empty statement, i.e., statements do not have to be delimited by a ';'
  # however, not delimiting statements with a ';' may lead to ambiguous/wrong programs...

  @weight(7)
  mult_stmt ("${stmt : Stmt};\n${rest : StmtList}") {
    loc stmt_breaks = (not (isNil stmt.symbols_break));

    rest.symbols_before = stmt.symbols_after;

    this.symbols_after = rest.symbols_after;
    this.symbols_break = (if .stmt_breaks stmt.symbols_break rest.symbols_break);
  }

}

@unit
@copy(symbols_before, in_global_block, in_loop, expected_return_type, symbols_after, symbols_break)
class Stmt {

  inh in_global_block : boolean;
  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  @weight(70)
  assign ("${lhs : DefVariableList} = ${rhs : ExprList}") {
    loc size = (Tuple:size lhs.defined_variables);

    lhs.names_in_list = (Set:empty);

    lhs.in_function = (not (isNil this.expected_return_type));

    # evaluation order is undefined if list has more than one expression!
    rhs.safe_only = (> .size 1);
    rhs.expected_types = (Tuple:toTypeTuple lhs.defined_variables);
    rhs.strict_tables = true;

    this.symbols_after =
      (SymbolTable:update this.symbols_before false lhs.defined_variables rhs.expr_types);
    this.symbols_break = nil;
  }

  @weight(30)
  local_assign ("${assign : LocalAssignment}") {
    assign.in_function = (not (isNil this.expected_return_type));

    this.symbols_break = nil;
  }

  @weight(30)
  @feature("functions,function_calls,control_flow")
  call_stmt ("${call : FunctionCall}") {
    call.expected_type = (Type:Any);

    this.symbols_after = this.symbols_before;
    this.symbols_break = nil;
  }

  @weight(30)
  @feature("control_flow")
  do ("do\+
        ${body : Block}\-
      end") {
    body.symbols_before = (SymbolTable:enterScope this.symbols_before);
  }

  @weight(2)
  @feature("control_flow")
  while ("while ${cond : Expr} do\+
            ${body : Block}\-
          end") {
    cond.safe_only = false;
    cond.expected_type = (Type:Boolean);
    cond.strict_tables = false;

    body.in_loop = true;
    body.symbols_before = (SymbolTable:enterScope this.symbols_before);

    this.symbols_after = this.symbols_before;
    this.symbols_break = nil; # NOTE: even if the body contains a break statement, it does not break
    # a possible outer loop
  }

  @weight(2)
  @feature("control_flow")
  repeat ("repeat\+
            ${body : Block}\-
          until ${cond : Expr}") {
    cond.expected_type = (Type:Boolean);
    cond.strict_tables = false;
    cond.symbols_before = body.symbols_after;
    cond.safe_only = false;

    body.in_loop = true;
    body.symbols_before = (SymbolTable:enterScope this.symbols_before);

    # we have to throw away all local variables that have been defined in the block, but we have to
    # keep all changes to global variables
    this.symbols_after = (if (isNil body.symbols_break) body.symbols_after body.symbols_break);
    
    this.symbols_break = nil; # NOTE: even if the body contains a break statement, it does not break
    # a possible outer loop
  }

  @feature("control_flow")
  for ("for ${loop_var : DefName} = ${lo : Expr}, ${hi : Expr} do\+
          ${body : Block}\-
        end") {
    loc loop_var_symbol = (Symbol:create (Symbol:getName loop_var.symbol) (Type:Number));

    loop_var.names_in_list = (Set:empty);

    loop_var.in_global_scope = false;
    loop_var.in_function = (not (isNil this.expected_return_type));

    lo.safe_only = true;
    lo.expected_type = (Type:Number);
    lo.strict_tables = false;

    hi.safe_only = true;
    hi.expected_type = (Type:Number);
    hi.strict_tables = false;

    body.in_loop = true;
    body.symbols_before =
      (SymbolTable:put (SymbolTable:enterScope this.symbols_before) .loop_var_symbol);

    this.symbols_after = this.symbols_before;
    this.symbols_break = nil; # NOTE: even if the body contains a break statement, it does not break
    # a possible outer loop
  }

  @weight(25)
  @feature("control_flow")
  if ("if ${cond : Expr} then\+
        ${then : Block}\-
      ${elseif : OptElseIfList}
      ${else : OptElse}
      end") {
    loc breaks =
      (or
        (not (isNil then.symbols_break))
        (not (isNil elseif.symbols_break))
        (not (isNil else.symbols_break))
      );

    cond.safe_only = false;
    cond.expected_type = (Type:Boolean);
    cond.strict_tables = false;

    then.symbols_before = (SymbolTable:enterScope this.symbols_before);

    this.symbols_after =
      (SymbolTable:common then.symbols_after
        (SymbolTable:common elseif.symbols_after else.symbols_after)
      );

    this.symbols_break =
      (if .breaks
        (SymbolTable:common
          (if (isNil then.symbols_break)
            then.symbols_after then.symbols_break)
          (SymbolTable:common
            (if (isNil elseif.symbols_break)
              elseif.symbols_after elseif.symbols_break)
            (if (isNil else.symbols_break)
              else.symbols_after else.symbols_break)
          )
        )
        nil
      );
  }

  @weight(8)
  @feature("functions,function_defs,control_flow")
  function ("${function : FunctionDefinition}") {
    function.in_function = (not (isNil this.expected_return_type));

    this.symbols_after = (SymbolTable:put this.symbols_before function.symbol function.global_def);
    this.symbols_break = nil;
  }

}

class LocalAssignment {

  inh in_function : boolean;
  inh in_global_block : boolean;
  inh symbols_before : SymbolTable;

  grd allowed;

  syn symbols_after : SymbolTable;

  @copy(symbols_before, in_function)
  local_assign ("local ${lhs : DefNameList} = ${rhs : ExprList}") {
    loc size = (Tuple:size lhs.defined_variables);

    this.allowed = (not this.in_global_block);

    lhs.names_in_list = (Set:empty);

    lhs.in_global_scope = false;

    # evaluation order is undefined if list has more than one expression!
    rhs.safe_only = (> .size 1);
    rhs.expected_types = (Tuple:toTypeTuple lhs.defined_variables);
    rhs.strict_tables = true;

    this.symbols_after =
      (SymbolTable:update this.symbols_before true lhs.defined_variables rhs.expr_types);
  }

}

class OptElseIfList {

  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  no_elseif ("") {
    this.symbols_after = this.symbols_before;
    this.symbols_break = nil;
  }

  @weight(2)
  @copy(in_loop, symbols_before, expected_return_type, symbols_after, symbols_break)
  elseif ("${elseif : ElseIfList}\n") {
    # intentionally left blank
  }

}

@list(6)
class ElseIfList {

  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  @copy(in_loop, symbols_before, expected_return_type, symbols_after, symbols_break)
  one_elseif ("${elseif : ElseIf}") {
    # intentionally left blank
  }

  @weight(2)
  @copy(in_loop, symbols_before, expected_return_type)
  mult_elseif ("${elseif : ElseIf}\n${rest : ElseIfList}") {
    loc breaks =
      (or
        (not (isNil elseif.symbols_break))
        (not (isNil rest.symbols_break))
      );

    this.symbols_after = (SymbolTable:common elseif.symbols_after rest.symbols_after);

    this.symbols_break =
      (if .breaks
        (SymbolTable:common
          (if (isNil elseif.symbols_break)
            elseif.symbols_after elseif.symbols_break)
          (if (isNil rest.symbols_break)
            rest.symbols_after rest.symbols_break)
        )
        nil
      );
  }

}

class ElseIf {

  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  @copy(in_loop, symbols_before, expected_return_type, symbols_after, symbols_break)
  elseif ("elseif ${cond : Expr} then\+${then : Block}") {
    cond.safe_only = false;
    cond.expected_type = (Type:Boolean);
    cond.strict_tables = false;

    then.symbols_before = (SymbolTable:enterScope this.symbols_before);
  }

}

class OptElse {

  inh in_loop : boolean;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbols_after : SymbolTable;
  syn symbols_break : SymbolTable;

  no_else ("") {
    this.symbols_after = this.symbols_before;
    this.symbols_break = nil;
  }

  @weight(2)
  @copy(in_loop, expected_return_type, symbols_after, symbols_break)
  else ("else\+${else : Block}\-") {
    else.symbols_before = (SymbolTable:enterScope this.symbols_before);
  }

}

class OptLastStatement {

  inh symbols_before : SymbolTable;
  inh in_loop : boolean;
  inh expected_return_type : Type;

  grd allowed;

  syn breaks : boolean;

  @weight(8)
  no_last_stmt ("") {
    this.allowed = true;
    this.breaks = false;
  }

  @copy(symbols_before, expected_return_type)
  @weight(3)
  ret ("${ret : ReturnStmt}") {
    this.allowed = (not (isNil this.expected_return_type));

    this.breaks = false;
  }

  @weight(10)
  break ("break") {
    this.allowed = this.in_loop;
    this.breaks = true;
  }

}

class ReturnStmt {

  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  @copy(symbols_before)
  ret ("return ${vals : OptExprList}") {
    vals.expected_types = (Type:toTuple this.expected_return_type);
    vals.strict_tables = true;
  }

}

@copy(symbols_before)
class FunctionDefinition {

  inh symbols_before : SymbolTable;
  inh in_function : boolean;

  grd allowed;

  syn symbol : Symbol;
  syn global_def : boolean;

  function (
      "function ${name : FunctionName}${body : FunctionBody}") {
    this.allowed = (not this.in_function);

    this.symbol = (Symbol:create name.name body.type);
    this.global_def = true;
  }

  @feature("local_funcs")
  local_function (
      "local function ${name : LocalFunctionName}${body : FunctionBody}") {
    this.allowed = true;

    this.symbol = (Symbol:create name.name body.type);
    this.global_def = false;
  }

}

class FunctionName {

  inh symbols_before : SymbolTable;

  grd valid;

  syn name : String;

  plain_name ("${name : Identifier}") {
    this.valid = (not (SymbolTable:contains this.symbols_before name.str));
    this.name = name.str;
  }

}

class LocalFunctionName {

  inh symbols_before : SymbolTable;

  grd valid;

  syn name : String;

  local_function_name ("${name : Identifier}") {
    this.valid = (not (SymbolTable:contains this.symbols_before name.str));
    this.name = name.str;
  }

}

class FunctionBody {

  inh symbols_before : SymbolTable;

  syn type : Type;

  function_body (
      "(${params : OptParamList})${return_type : Type}\+
          ${body : OptStmtList}
          ${return : ReturnStmt}\-
        end") {
    loc parameter_types = (Type:fromTuple (Tuple:toTypeTuple params.parameters));
    loc func_type = (Type:functionType .parameter_types return_type.type);

    return_type.tuple_allowed = true;

    body.symbols_before =
      (SymbolTable:putAll
        (SymbolTable:enterScope this.symbols_before)
        params.parameters
      );
    body.in_loop = false;
    body.in_global_block = false;
    body.expected_return_type = return_type.type;

    return.symbols_before = body.symbols_after;
    return.expected_return_type = return_type.type;
    
    this.type = .func_type;
  }

}

class OptParamList {

  syn parameters : Tuple;

  no_param_list ("") {
    this.parameters = (Tuple:empty);
  }

  @weight(3)
  @copy(parameters)
  param_list ("${params : ParamList}") {
    params.names_in_list = (Set:empty);
  }

}

@copy(names_in_list)
@list(10)
class ParamList {

  inh names_in_list : Set;

  syn parameters : Tuple;

  one_param ("${param : Param}") {
    this.parameters = (Tuple:from param.symbol);
  }

  mult_param ("${param : Param}, ${rest : ParamList}") {
    rest.names_in_list = (Set:add this.names_in_list (Symbol:getName param.symbol));

    this.parameters =
      (Tuple:prepend param.symbol rest.parameters);
  }

}

class Param {

  inh names_in_list : Set;

  grd unique;

  syn symbol : Symbol;

  param ("${id : Identifier}${type : Type}") {
    type.tuple_allowed = false;
    this.unique = (not (Set:contains this.names_in_list id.str));
    this.symbol = (Symbol:create id.str type.type);
  }

}

@hidden
@copy(type)
class Type {

  inh tuple_allowed : boolean;

  grd allowed;
  
  syn type : Type;

  @weight(10)
  primitive_type ("${type : PrimitiveType}") {
    this.allowed = true;
  }

  tuple_type ("Tuple(${type : TupleType})") {
    this.allowed = this.tuple_allowed;
    this.type = (Type:fromTuple type.type);
  }

  @weight(10)
  table_type ("Table(${type : TableType})") {
    this.allowed = true;
  }

}

@hidden
class PrimitiveType {

  syn type : Type;

  number ("Number") {
    this.type = (Type:Number);
  }

  string ("String") {
    this.type = (Type:String);
  }

  boolean ("Boolean") {
    this.type = (Type:Boolean);
  }

}

@hidden
@list(10)
class TupleType {

  syn type : Tuple;

  one_type ("${type : Type}") {
    type.tuple_allowed = false;
    this.type = (Tuple:from type.type);
  }

  mult_type ("${type : Type}, ${rest : TupleType}") {
    type.tuple_allowed = false;
    this.type = (Tuple:prepend type.type rest.type);
  }

}

@hidden
class TableType {

  syn type : Type;

  empty_table_type ("") {
    this.type = (Type:emptyTableType);
  }

  non_empty_table_type ("${fields : TableTypeFieldList}") {
    fields.names_in_list_before = (Set:empty);
    this.type = (Type:fromFieldTuple fields.fields);
  }

}

@hidden
@list(10)
@copy(names_in_list_before, names_in_list_after)
class TableTypeFieldList {
  
  inh names_in_list_before : Set;

  syn fields : Tuple;
  syn names_in_list_after : Set;

  one_field ("${field : TableTypeField}") {
    this.fields = (Tuple:from field.field);
  }

  mult_field ("${field : TableTypeField}, ${rest : TableTypeFieldList}") {
    rest.names_in_list_before = field.names_in_list_after;

    this.fields = (Tuple:prepend field.field rest.fields);
    this.names_in_list_after = rest.names_in_list_after;
  }

}

@hidden
class TableTypeField {

  inh names_in_list_before : Set;

  grd valid;

  syn field : TableField;
  syn names_in_list_after : Set;

  array_element ("${type : Type}") {
    this.valid = true;
    type.tuple_allowed = false;
    this.field = (TableField:tableFieldArray type.type); 
    this.names_in_list_after = this.names_in_list_before;
  }
  
  member ("${name : Identifier}: ${type : Type}") {
    this.valid = (not (Set:contains this.names_in_list_before name.str));
    type.tuple_allowed = false;
    this.field = (TableField:tableFieldMember name.str type.type);
    this.names_in_list_after = (Set:add this.names_in_list_before name.str);
  }

}

class OptExprList {

  inh symbols_before : SymbolTable;
  inh expected_types : Tuple;
  inh strict_tables : boolean;

  grd valid;

  no_expr_list ("") {
    loc expected_length = (Tuple:size this.expected_types);
    this.valid = (== .expected_length 0);
  }

  @copy(expected_types, symbols_before, strict_tables)
  expr_list ("${expr_list : ExprList}") {
    loc expected_length = (Tuple:size this.expected_types);
    this.valid = (> .expected_length 0);

    expr_list.safe_only = false; # ExprList enables 'safe_only' if list has more than one element
  }

}

@list(10)
@copy(symbols_before, strict_tables)
class ExprList {

  inh safe_only : boolean;
  inh expected_types : Tuple;
  inh symbols_before : SymbolTable;
  inh strict_tables : boolean;

  grd valid;
  
  syn expr_types : Tuple;

  @copy(safe_only)
  one_expr ("${expr : Expr}") {
    loc expected_length = (Tuple:size this.expected_types);

    this.valid = (== .expected_length 1);

    expr.expected_type = (Tuple:head this.expected_types);

    this.expr_types = (Tuple:from expr.type);
  }

  mult_expr ("${expr : Expr}, ${rest : ExprList}") {
    loc expected_length = (Tuple:size this.expected_types);

    this.valid = (> .expected_length 1);

    expr.safe_only = true;
    expr.expected_type = (Tuple:head this.expected_types);

    rest.safe_only = true;
    rest.expected_types = (Tuple:tail this.expected_types);

    this.expr_types = (Tuple:prepend expr.type rest.expr_types);
  }

}

class ArithmeticOp("+|-|*"); # NOTE: modulo may fail, ^ and / may overflow "arbitrarily"...

class EqualityOp("==|~=");

class RelationalOp("<|>|<=|>=");

class LogicalOp("and|or");

@copy(symbols_before, safe_only)
class Expr {

  inh safe_only : boolean;
  inh expected_type : Type;
  inh symbols_before : SymbolTable;
  inh strict_tables : boolean;

  grd valid;

  syn type : Type;

  @weight(3)
  @copy(expected_type, type, strict_tables)
  atom ("${atom : ExprAtom}") {
    this.valid = true;
  }

  @weight(7)
  binop_arithmetic ("(${lhs : Expr}) ${op : ArithmeticOp} (${rhs : Expr})") {
    loc type = (Type:Number);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:Number);
    rhs.expected_type = (Type:Number);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  binop_equality ("(${lhs : Expr}) ${op : EqualityOp} (${rhs : Expr})") {
    loc type = (Type:Boolean);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:Any);
    rhs.expected_type = (Type:Any);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(7)
  binop_relational ("(${lhs : Expr}) ${op : RelationalOp} (${rhs : Expr})") {
    loc type = (Type:Boolean);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:Number);
    rhs.expected_type = (Type:Number);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  binop_relational_string ("(${lhs : Expr}) ${op : RelationalOp} (${rhs : Expr})") {
    loc type = (Type:Boolean);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:String);
    rhs.expected_type = (Type:String);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(3)
  binop_logical ("(${lhs : Expr}) ${op : LogicalOp} (${rhs : Expr})") {
    loc type = (Type:Boolean);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:Boolean);
    rhs.expected_type = (Type:Boolean);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  concat ("(${lhs : Expr}) .. (${rhs : Expr})") {
    loc type = (Type:String);

    this.valid = (Type:assignable .type this.expected_type);

    lhs.expected_type = (Type:String);
    rhs.expected_type = (Type:String);

    lhs.strict_tables = false;
    rhs.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  unary_minus ("-(${expr : Expr})") {
    loc type = (Type:Number);

    this.valid = (Type:assignable .type this.expected_type);

    expr.expected_type = (Type:Number);
    expr.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  unary_logical_not ("not (${expr : Expr})") {
    loc type = (Type:Boolean);

    this.valid = (Type:assignable .type this.expected_type);

    expr.expected_type = (Type:Boolean);
    expr.strict_tables = false;

    this.type = .type;
  }

  @weight(1)
  unary_length ("#(${expr : Expr})") {
    loc type = (Type:Number);

    this.valid = (Type:assignable .type this.expected_type);

    expr.expected_type = (Type:String); # NOTE: length of tables is not well defined...
    expr.strict_tables = false;

    this.type = .type;
  }

}

@copy(safe_only, expected_type, symbols_before, strict_tables)
class ExprAtom {

  inh safe_only : boolean;
  inh expected_type : Type;
  inh symbols_before : SymbolTable;
  inh strict_tables : boolean;

  grd valid;

  syn type : Type;

  @weight(1)
  _true ("true") {
    loc type = (Type:Boolean);
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(1)
  _false ("false") {
    loc type = (Type:Boolean);
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(2)
  _nil ("nil") {
    loc type = (Type:Nil);
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(8)
  numeral ("${numeral : Numeral}") {
    loc type = numeral.type;
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(2)
  literal_string ("${string : StringLiteral}") {
    loc type = (Type:String);
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(6)
  prefix_expr ("${expr : PrefixExpr}") {
    loc type = expr.type;
    this.valid = (Type:assignable .type this.expected_type);
    this.type = .type;
  }


  @weight(1)
  @feature("tables")
  table_constructor ("${table : TableConstructor}") {
    loc type = table.type;
    this.valid = true; # further checks in TableConstructor productions
    this.type = .type;
  }
  
}

class TableConstructor {

  inh expected_type : Type;
  inh symbols_before : SymbolTable;

  grd allowed;

  syn type : Type;

  table_empty ("{}") {
    loc type = (Type:emptyTableType);

    this.allowed = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  @weight(20)
  @copy(symbols_before)
  table_non_empty ("{${fields : FieldList}}") {
    loc expect_table = (Type:isTableType this.expected_type);

    this.allowed = (or
      (Type:assignable (Type:anyTableType) this.expected_type)
      .expect_table
    );

    fields.expected_fields = (if .expect_table this.expected_type (Type:anyTableType));
    fields.names_in_list = (Set:empty);

    this.type = (Type:fromFieldTuple fields.fields);
  }

}

class FieldSep(",|;");

@list(20)
@copy(symbols_before)
class FieldList {

  inh expected_fields : Type; # TableType
  inh names_in_list : Set;
  inh symbols_before : SymbolTable;

  grd allowed;

  syn fields : Tuple;

  one_field ("${field : Field}") {
    this.allowed = (<= (Type:tableSize this.expected_fields) 1);

    field.expected_fields_before = this.expected_fields;
    field.names_in_list_before = this.names_in_list;

    this.fields = (Tuple:from field.field);
  }

  @weight(4)
  mult_field ("${field : Field}${sep : FieldSep} ${rest : FieldList}") {
    this.allowed = true;

    field.expected_fields_before = this.expected_fields;
    field.names_in_list_before = this.names_in_list;

    rest.expected_fields = field.expected_fields_after;
    rest.names_in_list = field.names_in_list_after;
    
    this.fields = (Tuple:prepend field.field rest.fields);
  }

}

@copy(expected_fields_before, symbols_before, expected_fields_after, names_in_list_after)
class Field {

  inh expected_fields_before : Type; # TableType
  inh names_in_list_before : Set;
  inh symbols_before : SymbolTable;

  grd allowed;

  syn expected_fields_after : Type; # TableType
  syn names_in_list_after : Set;
  syn field : TableField;

  array_field ("${field : ArrayField}") {
    loc table_size = (Type:tableSize this.expected_fields_before);
    loc number_of_array_elements = (Type:numberOfArrayElements this.expected_fields_before);

    this.allowed = (or
      (> .number_of_array_elements 0) # still some array elements required
      (== (- .table_size .number_of_array_elements) 0) # no other fields required
    );

    this.names_in_list_after = this.names_in_list_before;

    this.field = (TableField:tableFieldArray field.type); 
  }

  @copy(names_in_list_before)
  member_field ("${member : FieldName} = ${expr : Expr}") {
    loc table_size = (Type:tableSize this.expected_fields_before);
    loc number_of_members = (Type:numberOfMembers this.expected_fields_before);

    loc member_name = member.name;

    this.allowed = (or
      (> .number_of_members 0) # still some members required
      (== (- .table_size .number_of_members) 0) # no other fields required
    );

    expr.safe_only = true;
    expr.expected_type = member.type;
    expr.strict_tables = true;

    this.field = (TableField:tableFieldMember .member_name expr.type); 
  }

}

@copy(symbols_before, type)
class ArrayField {

  inh expected_fields_before : Type; # TableType
  inh symbols_before : SymbolTable;

  grd allowed;

  syn expected_fields_after : Type; # TableType
  syn type : Type;

  array_field_concrete ("${expr : Expr}") {
    loc number_of_array_elements = (Type:numberOfArrayElements this.expected_fields_before);

    this.allowed = (and
      (> .number_of_array_elements 0)
      (not (Type:hasExpectedArrayElement this.expected_fields_before))
    );

    expr.safe_only = true;
    expr.expected_type = (Type:getFirstArrayElement this.expected_fields_before);
    expr.strict_tables = true;

    this.expected_fields_after = (Type:removeMember this.expected_fields_before);
  }

  array_field_expected ("${expr : Expr}") {
    this.allowed = (Type:hasExpectedArrayElement this.expected_fields_before);

    expr.safe_only = true;
    expr.expected_type = (Type:expectedArrayElement this.expected_fields_before);
    expr.strict_tables = true;

    this.expected_fields_after = (Type:removeExpectedArrayElement this.expected_fields_before);
  }

  array_field_new ("${expr : Expr}") {
    loc number_of_array_elements = (Type:numberOfArrayElements this.expected_fields_before);

    this.allowed = (== .number_of_array_elements 0);

    expr.safe_only = true;
    expr.expected_type = (Type:Any);
    expr.strict_tables = true;

    this.expected_fields_after = this.expected_fields_before;
  }

}

class FieldName {

  inh expected_fields_before : Type;
  inh names_in_list_before : Set;

  grd allowed;
  grd valid;

  syn name : String;
  syn type : Type;

  syn expected_fields_after : Type;
  syn names_in_list_after : Set;

  field_name_concrete ("${member : TableMember}") {
    loc number_of_members = (Type:numberOfMembers this.expected_fields_before);

    loc member_name = (Symbol:getName member.symbol);
    loc member_type = (Symbol:getType member.symbol);

    this.allowed = (and
      (> .number_of_members 0)
      (not (Type:hasExpectedNamedMember this.expected_fields_before))
    );
    this.valid = true;

    member.table_type = this.expected_fields_before;
    member.expected_type = nil;

    this.name = .member_name;
    this.type = .member_type;

    this.expected_fields_after = (Type:removeMember this.expected_fields_before .member_name);
    this.names_in_list_after = (Set:add this.names_in_list_before .member_name);
  }

  field_name_expected ("${id : Identifier}") {
    loc member_name = id.str;

    this.allowed = (Type:hasExpectedNamedMember this.expected_fields_before);
    this.valid = (not (Set:contains this.names_in_list_before .member_name));

    this.name = .member_name;
    this.type = (Type:expectedNamedMember this.expected_fields_before);

    this.expected_fields_after = (Type:removeExpectedNamedMember this.expected_fields_before);
    this.names_in_list_after = (Set:add this.names_in_list_before .member_name);
  }

  field_name_new ("${id : Identifier}") {
    loc number_of_members = (Type:numberOfMembers this.expected_fields_before);
    loc member_name = id.str;

    this.allowed = (== .number_of_members 0);
    this.valid = (not (Set:contains this.names_in_list_before .member_name));

    this.name = .member_name;
    this.type = (Type:Any);

    this.expected_fields_after = this.expected_fields_before;
    this.names_in_list_after = (Set:add this.names_in_list_before .member_name);
  }

}

@copy(safe_only, expected_type, symbols_before, type)
class PrefixExpr {

  inh safe_only : boolean;
  inh expected_type : Type;
  inh symbols_before : SymbolTable;
  inh strict_tables : boolean;

  grd allowed;

  syn type : Type;

  @weight(1)
  var ("${var : UseVariable}") {
    this.allowed = true;
    var.strict_tables = this.strict_tables;
  }

  @weight(8)
  @feature("functions,function_calls")
  function_call ("${call : FunctionCall}") {
    this.allowed = (not this.safe_only);
  }

  @weight(11)
  paren_expr ("(${expr : Expr})") {
    this.allowed = true;
    expr.strict_tables = this.strict_tables;
  }

}

@list(10)
@copy(names_in_list, symbols_before, in_function)
class DefVariableList {

  inh in_function : boolean;
  inh names_in_list : Set;
  inh symbols_before : SymbolTable;

  syn defined_variables : Tuple;

  @weight(10)
  one_var ("${var : DefVariable}") {
    this.defined_variables = (Tuple:from var.symbol);
  }

  mult_var ("${var : DefVariable}, ${rest : DefVariableList}") {
    rest.names_in_list = (Set:add this.names_in_list (Symbol:getName var.symbol));

    this.defined_variables =
      (Tuple:prepend var.symbol rest.defined_variables);
  }

}

@copy(symbols_before)
class DefVariable {

  inh in_function : boolean;
  inh names_in_list : Set;
  inh symbols_before : SymbolTable;

  grd allowed;

  syn symbol : Symbol;

  @weight(1)
  @copy(names_in_list, in_function, symbol)
  plain_var ("${name : DefName}") {
    this.allowed = true;

    name.in_global_scope = true;
  }

  @weight(1)
  table_access_index ("${table : PrefixExpr}[${element : ArrayElement}]") {
    # only allow one table access in a DefVariableList (we cannot guarantee unique members
    # otherwise)
    this.allowed = (not (Set:contains this.names_in_list nil));

    table.safe_only = true;
    table.expected_type = (Type:anyTableTypeWithArrayElement (Type:Any));
    table.strict_tables = false;

    element.table_type = table.type;
    element.expected_type = (Type:Any);

    this.symbol = (Symbol:create nil (Type:typeOf table.type element.index));
  }

  @weight(1)
  table_access_member ("${table : PrefixExpr}.${member : TableMember}") {
    # only allow one table access in a DefVariableList (we cannot guarantee unique members
    # otherwise)
    this.allowed = (not (Set:contains this.names_in_list nil));

    table.safe_only = true;
    table.expected_type = (Type:anyTableTypeWithMember (Type:Any));
    table.strict_tables = false;

    member.table_type = table.type;
    member.expected_type = (Type:Any);

    this.symbol = (Symbol:create nil (Symbol:getType member.symbol));
  }

}

@copy(symbols_before, expected_type)
class UseVariable {

  inh safe_only : boolean;
  inh expected_type : Type;
  inh symbols_before : SymbolTable;
  inh strict_tables : boolean;

  syn type : Type;

  @weight(5)
  plain_var ("${name : UseName}") {
    loc type = (Symbol:getType name.symbol);
    name.strict_tables = this.strict_tables;
    this.type = (Symbol:getType name.symbol);
  }

  @weight(13)
  @copy(safe_only)
  table_access_index ("${table : PrefixExpr}[${element : ArrayElement}]") {
    table.expected_type = (Type:anyTableTypeWithArrayElement this.expected_type);
    table.strict_tables = false;
    element.table_type = table.type;

    this.type = (Type:typeOf table.type element.index);
  }

  @weight(13)
  @copy(safe_only)
  table_access_member ("${table : PrefixExpr}.${member : TableMember}") {
    table.expected_type = (Type:anyTableTypeWithMember this.expected_type);
    table.strict_tables = false;
    member.table_type = table.type;

    this.type = (Symbol:getType member.symbol);
  }

}

class ArrayElement {

  inh table_type : Type;
  inh expected_type : Type;

  syn index : int;

  array_element (Type:getArrayElements this.table_type this.expected_type) : Integer {
    this.index = $;
  }

}

class TableMember {

  inh table_type : Type;
  inh expected_type : Type;

  syn symbol : Symbol;

  table_member (Type:getTableMembers this.table_type this.expected_type) : Symbol {
    this.symbol = $;
  }

}

class FunctionCall {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  @copy(symbols_before)
  function_call ("${callee : Callee}${args : Arguments}") {
    loc func_type = (Symbol:getType callee.symbol);

    callee.expected_return_type = this.expected_type;
    args.parameter_types = (Type:getParameterTypes .func_type);

    this.type = (Type:getReturnType .func_type);
  }

}

class Callee {

  inh symbols_before : SymbolTable;
  inh expected_return_type: Type;

  syn symbol : Symbol;

  callee (SymbolTable:visibleSymbols this.symbols_before
      (Type:anyFunctionType this.expected_return_type) false) : Symbol {
    this.symbol = $;
  }

}

class Arguments {

  inh symbols_before : SymbolTable;
  inh parameter_types : Type;

  grd allowed;

  @copy(symbols_before)
  expr_args ("(${exprs : OptExprList})") {
    this.allowed = true;

    exprs.expected_types = (Type:toTuple this.parameter_types);
    exprs.strict_tables = true;
  }

}

class Numeral {

  syn type : Type;

  int_numeral ("${num : IntNumber}") {
    this.type = (Type:Number);
  }

  float_numeral ("${num : FloatNumber}") {
    this.type = (Type:Number);
  }

}

@list(10)
@copy(names_in_list, symbols_before, in_global_scope, in_function)
class DefNameList {

  inh names_in_list : Set;
  inh symbols_before : SymbolTable;

  inh in_global_scope : boolean;
  inh in_function : boolean;

  syn defined_variables : Tuple;

  @weight(10)
  one_name ("${name : DefName}") {
    this.defined_variables = (Tuple:from name.symbol);
  }

  mult_name ("${name : DefName}, ${rest : DefNameList}") {
    rest.names_in_list = (Set:add this.names_in_list (Symbol:getName name.symbol));

    this.defined_variables =
      (Tuple:prepend name.symbol rest.defined_variables);
  }

}

class DefName {

  inh symbols_before : SymbolTable;
  inh names_in_list : Set;

  inh in_function : boolean;
  inh in_global_scope : boolean;

  grd allowed;
  grd unique;

  syn symbol : Symbol;

  def_name ("${id : Identifier}") {
    loc already_defined = (SymbolTable:contains this.symbols_before id.str);

    loc type =
      (if .already_defined
        (Symbol:getType (SymbolTable:get this.symbols_before id.str))
        (Type:Any));

    this.unique = (not (Set:contains this.names_in_list id.str));
    this.allowed =
      (and
        (or
          .already_defined
          (not (and this.in_global_scope this.in_function)))
        (not (Type:isFunctionType .type)));

    this.symbol = (Symbol:create id.str .type);
  }

}

class UseName {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh strict_tables : boolean;
  
  syn symbol : Symbol;

  use_name (SymbolTable:visibleSymbols this.symbols_before this.expected_type this.strict_tables) : Symbol {
    this.symbol = $;
  }

}

@count(50)
class Identifier("[a-z][a-zA-Z0-9_]{2,5}");

@count(1000)
class IntNumber("0|[1-9][0-9]{0,6}");

@count(1000)
class FloatNumber("[0-9]{1,4}.[0-9]{1,4}");

@count(1000)
class StringLiteral("'[0-9a-zA-Z .]{0,35}'");
