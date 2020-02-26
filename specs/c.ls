use Labels;
use Symbol;
use SymbolTable;
use Type;

class Program {

  program (
      "#include <stdint.h>\n\n
      ${decls : OptionalGlobalDeclarationList}
      ${main : MainDeclaration}") {
    decls.types_before = (SymbolTable:empty);
    decls.symbols_before = (SymbolTable:empty);

    main.types_before = decls.types_after;
    main.symbols_before = decls.symbols_after;
  }

}

@copy
class OptionalGlobalDeclarationList {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  syn types_after : SymbolTable;
  syn symbols_after : SymbolTable;
  
  no_decls ("") {
    this.types_after = this.types_before;
    this.symbols_after = this.symbols_before;
  }

  @weight(100)
  decls ("${decls : GlobalDeclarationList}\n") {
    # intentionally left blank
  }

}

@copy
@list(200)
class GlobalDeclarationList {

  syn types_after : SymbolTable;
  syn symbols_after : SymbolTable;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  @weight(1)
  one_decl ("${decl : GlobalDeclaration}") {
    this.types_after = decl.types_after;
    this.symbols_after = decl.symbols_after;
  }

  @weight(10)
  mult_decl ("${decl : GlobalDeclaration}\n
              ${rest : GlobalDeclarationList}") {
    rest.types_before = decl.types_after;
    rest.symbols_before = decl.symbols_after;

    this.types_after = rest.types_after;
    this.symbols_after = rest.symbols_after;
  }

}

@copy
class GlobalDeclaration {

  syn types_after : SymbolTable;
  syn symbols_after : SymbolTable;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  global_type_decl ("${type_decl : TypeDeclaration}") {
    this.types_after = (SymbolTable:put this.types_before type_decl.type_symbol);
    this.symbols_after = this.symbols_before;
  }

  @weight(1)
  global_var_decl ("${var_decl : VariableDeclaration}\n") {
    var_decl.global_variable = true;
    var_decl.inside_composite = false;

    this.types_after = this.types_before;
    this.symbols_after = (SymbolTable:put this.symbols_before var_decl.symbol);
  }

  @weight(2)
  global_func_decl ("${func_decl : FunctionDeclaration}") {
    this.types_after = this.types_before;
    this.symbols_after = (SymbolTable:put this.symbols_before func_decl.symbol);
  }

}

@copy
class TypeDeclaration {

  syn type_symbol : Symbol;

  inh types_before : SymbolTable;

  packed_struct_decl
      ("#pragma pack(push)\n
        #pragma pack(1)\n
        struct ${name : DefIdentifier} {\+${members : VariableDeclarationList}\-};\n
        #pragma pack(pop)\n") {
    loc struct_type = (Type:createStructType name.name members.symbols_after);

    name.symbols_before = this.types_before; # sic

    members.symbols_before = (SymbolTable:empty);
    members.inside_composite = true;

    this.type_symbol = (Symbol:create name.name .struct_type);
  }

  struct_decl ("struct ${name : DefIdentifier} {\+${members : VariableDeclarationList}\-};\n") {
    loc struct_type = (Type:createStructType name.name members.symbols_after);

    name.symbols_before = this.types_before; # sic

    members.symbols_before = (SymbolTable:empty);
    members.inside_composite = true;

    this.type_symbol = (Symbol:create name.name .struct_type);
  }

  union_decl ("union ${name : DefIdentifier} {\+${members : VariableDeclarationList}\-};\n") {
    loc union_type = (Type:createUnionType name.name members.symbols_after);
    name.symbols_before = this.types_before; # sic

    members.symbols_before = (SymbolTable:empty);
    members.inside_composite = true;

    this.type_symbol = (Symbol:create name.name .union_type);
  }

}

@copy
class VariableDeclaration {
  
  syn symbol : Symbol;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh inside_composite : boolean;
  inh global_variable : boolean;

  grd allowed;
  grd valid;

  @weight(3)
  var_decl_flat ("${type : Type} ${name : DefIdentifier};") {
    this.allowed = true;
    this.valid = true;

    this.symbol = (Symbol:create name.name type.type);
  }

  var_decl_flat_init ("${type : Type} ${name : DefIdentifier} = ${init : Expr};") {
    loc symbol = (Symbol:create name.name type.type);

    this.allowed = (not this.inside_composite);
    this.valid = true;

    this.symbol = .symbol;

    init.symbols_before = (SymbolTable:put this.symbols_before .symbol);
    init.expected_type = type.type;
    init.safe_only = this.global_variable;
  }

  var_decl_array ("${type : Type} ${name : DefIdentifier}${dims : ArrayDimensions};") {
    this.allowed = true;
    this.valid = true;

    dims.base_type = type.type;
    this.symbol = (Symbol:create name.name dims.type);
  }

  var_decl_flat_bitfield
    ("${cnst : ConstQualifier}${vol : VolatileQualifier}${type : PrimitiveType} 
      ${name : DefIdentifier} : ${size : SmallIntNumber};") {
    loc size = (int size.str);

    this.allowed = this.inside_composite;
    this.valid =
      (and
        (Type:isIntType type.type)
        (<= .size type.width)
      );
    
    cnst.qualifiers_allowed = true;
    vol.qualifiers_allowed = true;

    type.const = cnst.is_const;

    this.symbol =
        (Symbol:create name.name (Type:createBitFieldType type.type .size));
  }

}

@copy
class OptionalVariableDeclarationList {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh inside_composite : boolean;

  syn symbols_after : SymbolTable;

  no_decls ("") {
    this.symbols_after = this.symbols_before;
  }

  @weight(100)
  decls ("${decls : VariableDeclarationList}\n") {
    # intentionally left blank
  }

}

@copy
@unit
@list(10)
class VariableDeclarationList {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh inside_composite : boolean;

  syn symbols_after : SymbolTable;

  @weight(1)
  one_decl ("${decl : VariableDeclaration}") {
    decl.global_variable = false;

    this.symbols_after = (SymbolTable:put this.symbols_before decl.symbol);
  }

  @weight(3)
  mult_decl ("${decl : VariableDeclaration}\n${rest : VariableDeclarationList}") {
    decl.global_variable = false;
    rest.symbols_before = (SymbolTable:put this.symbols_before decl.symbol);

    this.symbols_after = rest.symbols_after;
  }

}

class ParameterDeclaration {

  syn symbol : Symbol;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  @copy
  param_decl ("${type : Type} ${name : DefIdentifier}") {
    this.symbol = (Symbol:create name.name type.type);
  }

}

@copy
@list(10)
class ParameterDeclarationList {

  syn type : Type;
  syn symbols_after : SymbolTable;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  one_param ("${param : ParameterDeclaration}") {
    this.symbols_after = (SymbolTable:put this.symbols_before param.symbol);
    this.type = (Type:createTupleType (Symbol:getType param.symbol));
  }

  mult_param ("${param : ParameterDeclaration}, ${rest : ParameterDeclarationList}") {
    rest.symbols_before = (SymbolTable:put this.symbols_before param.symbol);
    this.type =
      (Type:mergeTupleTypes (Type:createTupleType (Symbol:getType param.symbol)) rest.type);
  }

}

class FunctionDeclaration {

  syn symbol : Symbol;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  @copy
  func_decl
      ("${ret_type : ReturnType} ${name : DefIdentifier}(${params : ParameterDeclarationList}) {\+
          ${body : FunctionBody}\-
        }\n") {
    params.symbols_before = (SymbolTable:enterScope this.symbols_before);
    body.symbols_before = params.symbols_after;
    body.expected_return_type = ret_type.type;
    this.symbol = (Symbol:create name.name (Type:createFunctionType ret_type.type params.type));
  }

}

class MainDeclaration {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;

  @copy
  main ("int main(void) {\+${body : FunctionBody}\-}\n") {
    body.symbols_before = (SymbolTable:enterScope this.symbols_before);
    body.expected_return_type = (Type:intType false);
  }

}

@unit
class FunctionBody {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  @copy
  body ("
      ${block : Block}
      ${labels : MissingLabels}${ret : OptionalReturnStatement}") {
    block.inside_loop = false;

    ret.symbols_before = block.symbols_after;

    block.labels_before = (Labels:empty);
    labels.labels_before = block.labels_after;
    
    ret.return_required = (> (Labels:missingCount block.labels_after) 0);
  }

}

class MissingLabels {

  inh labels_before : Labels;

  missing_labels ("#{(Labels:missingLabels this.labels_before)}") {
    # intentionally left blank
  }

}

class Block {

  syn symbols_after : SymbolTable;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  @copy
  block ("${vars : OptionalVariableDeclarationList}
          ${stmts : OptionalStmtList}") {
    vars.inside_composite = false;
    stmts.symbols_before = vars.symbols_after;
  }

}

@copy
class OptionalStmtList {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  no_stmts ("") {
    this.labels_after = this.labels_before;
  }

  @weight(100)
  stmts ("${stmts : StmtList}") {
    # intentionally left blank
  }

}

@copy
@list(50)
class StmtList {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  @weight(2)
  one_stmt ("${stmt : Stmt}") {
    # intentionally left blank
  }

  @weight(7)
  mult_stmt ("${stmt : Stmt}\n${rest : StmtList}") {
    rest.labels_before = stmt.labels_after;
    this.labels_after = rest.labels_after;
  }

}

@copy
class Stmt {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  grd possible;

  @weight(18)
  assign ("${assign : AssignStmt}") {
    this.possible = true;

    this.labels_after = this.labels_before;
  }

  @weight(2)
  ret ("${ret : ReturnStatement}") {
    this.possible = true;

    this.labels_after = this.labels_before;
  }

  break ("break;") {
    this.possible = this.inside_loop;

    this.labels_after = this.labels_before;
  }

  continue ("continue;") {
    this.possible = this.inside_loop;

    this.labels_after = this.labels_before;
  }

  @weight(6)
  call ("${call : Call};") {
    this.possible = true;

    call.expected_type = (Type:anyType);
    this.labels_after = this.labels_before;
  }

  @weight(4)
  if_then ("if (${cond : Condition}) {\+${then : Block}\-}") {
    this.possible = true;
    
    cond.safe_only = false;
  }

  @weight(4)
  if_then_else ("if (${cond : Condition}) {\+${then : IfHelper}\-} else {\+${else : Block}\-}") {
    this.possible = true;

    cond.safe_only = false;
    else.labels_before = then.labels_after;
    this.labels_after = else.labels_after;
  }

  @weight(4)
  while ("while (${cond : Condition}) {\+${body : Block}\-}") {
    this.possible = true;

    body.inside_loop = true;
    cond.safe_only = false;
  }

  @weight(4)
  do_while ("do {\+${body : Block}\-} while (${cond : Condition});") {
    this.possible = true;

    body.inside_loop = true;
    cond.safe_only = false;
  }

  @weight(4)
  for ("for (${type : Type} ${name : DefIdentifier} = ${init : Expr}; 
          ${cond : Condition}; ${update : Expr}) {\+
          ${body : Block}\-
        }") {
    loc new_symbols = 
      (SymbolTable:put
        (SymbolTable:enterScope this.symbols_before)
        (Symbol:create name.name type.type)
      );

    this.possible = true;

    name.symbols_before = (SymbolTable:enterScope this.symbols_before);

    init.symbols_before = .new_symbols;
    init.expected_type = type.type;
    init.safe_only = false;

    cond.symbols_before = .new_symbols;
    cond.safe_only = false;

    update.symbols_before = .new_symbols;
    update.expected_type = (Type:anyType);
    update.safe_only = false;

    body.symbols_before = (SymbolTable:enterScope .new_symbols);

    body.inside_loop = true;
  }

  control_flow_statement ("${cfs : ControlFlowStmt}") {
    this.possible = true;
  }

}

@copy
@unit
class ControlFlowStmt {

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  label ("${label : DefLabel}: ${stmt : Stmt}") {
    stmt.labels_before = label.labels_after;
    this.labels_after = stmt.labels_after;
  }

  goto_forwards ("goto ${label : UseLabel};") {
    # intentionally left blank
  }

}

# NOTE: differentiating between forward and backward labels/jumps allows us to use different
# weights and makes backward labels/jumps more likely

@count(100)
class LabelName("L[a-zA-Z0-9_]{2,5}");

@unit
class DefLabel {

  inh labels_before : Labels;

  syn labels_after : Labels;

  grd possible;
  grd valid;

  label ("${label : LabelName}") {
    this.possible = true;
    this.valid = (and
        (not (Labels:isDefined this.labels_before label.str))
        (not (Labels:isMissing this.labels_before label.str))
      );

    this.labels_after = (Labels:handleLabel this.labels_before label.str);
  }

  @weight(3)
  label_missing ("${label : LabelName}") {
    this.possible = (>= (Labels:missingCount this.labels_before) 1);
    this.valid = (and
        (not (Labels:isDefined this.labels_before label.str))
        (Labels:isMissing this.labels_before label.str)
      );

    this.labels_after = (Labels:handleLabel this.labels_before label.str);
  }

}

@unit
class UseLabel {

  inh labels_before : Labels;

  syn labels_after : Labels;

  grd possible;
  grd valid;

  label_forwards ("${label : LabelName}") {
    this.possible = true;
    this.valid = (not (Labels:isDefined this.labels_before label.str));

    this.labels_after = (Labels:handleGoto this.labels_before label.str);
  }

  label_backwards ("${label : LabelName}") {
    this.possible = (>= (Labels:definedCount this.labels_before) 1);
    this.valid = (Labels:isDefined this.labels_before label.str);

    this.labels_after = (Labels:handleGoto this.labels_before label.str);
  }

}

class IfHelper {

  syn symbols_after : SymbolTable;

  inh types_before : SymbolTable;
  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh inside_loop : boolean;

  inh labels_before : Labels;
  syn labels_after : Labels;

  @copy
  block ("${block : Block}") {
    # intentionally left blank
  }

}

class ShorthandOperator("+=|-=|*=|/=");

class ShorthandOperatorInt("%=|<<=|>>="); 

@copy
class AssignStmt {

  inh symbols_before : SymbolTable;

  grd valid; 

  assign ("${lhs : LValue} = ${rhs : Expr};") {
    lhs.expected_type = (Type:anyType);
    rhs.expected_type = lhs.type;

    lhs.const_allowed = false;
    lhs.bitfield_allowed = true;

    rhs.safe_only = false;

    this.valid = true;
  }

  shorthand ("${lhs : LValue} ${op : ShorthandOperator} ${rhs : Expr};") {
    lhs.expected_type = (Type:anyNumberType);
    rhs.expected_type = lhs.type;

    lhs.const_allowed = false;
    lhs.bitfield_allowed = true;

    rhs.safe_only = false;

    this.valid = (Type:isNumberType lhs.type);
  }

  shorthand_int ("${lhs : LValue} ${op : ShorthandOperatorInt} ${rhs : Expr};") {
    lhs.expected_type = (Type:intType false);
    rhs.expected_type = lhs.type;

    lhs.const_allowed = false;
    lhs.bitfield_allowed = true;

    rhs.safe_only = false;

    this.valid = true;
  }

}

@copy
class LValue {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh const_allowed : boolean;
  inh bitfield_allowed : boolean;

  syn type : Type;

  grd type_assignable;
  grd type_matches;
  grd const_matches;

  @weight(2)
  lhs_var ("${var : UseIdentifier}") {
    loc var_type = (Symbol:getType var.symbol);

    this.type = .var_type;

    this.type_assignable = (Type:isAssignableType .var_type);
    this.type_matches =
      (and
        (Type:assignable .var_type this.expected_type)
        (Type:bitFieldAllowed .var_type this.bitfield_allowed)
      );

    this.const_matches =
      (or
        this.const_allowed
        (not (Type:isConst .var_type))
      );
  }
  
  lhs_array ("${array : ArrayAccess}") {
    loc array_type = (Symbol:getType array.symbol);
    loc array_base_type = (Type:getArrayBaseType .array_type);

    this.type = .array_base_type;

    this.type_assignable = (Type:isAssignableType .array_base_type);
    this.type_matches =
      (and
        (Type:assignable .array_base_type this.expected_type)
        (Type:bitFieldAllowed .array_base_type this.bitfield_allowed)
      );

    this.const_matches = true; # checked in 'ArrayAccess'
  }

  lhs_deref ("${deref : LValueDeref}") {
    deref.min_pointer_depth = 0;

    this.type = deref.type;

    this.type_assignable = (Type:isAssignableType deref.type);
    this.type_matches =
      (and
        (Type:assignable deref.type this.expected_type)
        (Type:bitFieldAllowed deref.type this.bitfield_allowed)
      );

    this.const_matches =
      (or
        this.const_allowed
        (not (Type:isConst deref.type))
      );
  }

  lhs_member ("${access : MemberAccess}") {
    loc access_type = (Symbol:getType access.symbol);

    this.type = .access_type;

    this.type_assignable = (Type:isAssignableType .access_type);
    this.type_matches =
      (and
        (Type:assignable .access_type this.expected_type)
        (Type:bitFieldAllowed .access_type this.bitfield_allowed)
      );

    this.const_matches = true; # checked in 'MemberAccess'
  }

}

@copy
@list(4)
class LValueDeref {

  inh symbols_before : SymbolTable;
  inh min_pointer_depth : int;

  syn type : Type;

  grd valid;

  @weight(2)
  one_deref_var ("*(${var : UseIdentifier})") {
    loc var_type = (Symbol:getType var.symbol);

    var.expected_type = nil;

    this.type = (Type:getPointeeType .var_type);

    this.valid =
      (>=
        (Type:getPointerDepth .var_type)
        (+ this.min_pointer_depth 1)
      );
  }

  mult_deref ("*(${deref : LValueDeref})") {
    deref.min_pointer_depth = (+ this.min_pointer_depth 1);

    this.type = (Type:getPointeeType deref.type);

    this.valid = true; # checked in 'base case'
  }

}

@copy
class OptionalReturnStatement {

  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;
  inh return_required : boolean;

  grd valid;

  no_ret ("") {
    this.valid = (not this.return_required);
  }

  @weight(20)
  ret ("\n${ret : ReturnStatement}") {
    this.valid = true;
  }

}

class ReturnStatement {

  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  grd type_matches;

  ret_void ("return;") {
    this.type_matches = (Type:equals this.expected_return_type (Type:voidType));
  }

  @copy
  ret_val ("return ${val : Expr};") {
    val.expected_type = this.expected_return_type;
    val.safe_only = false;

    this.type_matches = (not (Type:equals this.expected_return_type (Type:voidType)));
  }

}

class Condition {

  inh symbols_before : SymbolTable;
  inh safe_only : boolean;

  @copy
  cond_expr ("${expr : Expr}") {
    expr.expected_type = (Type:intType false);
  }

}

class ArithOperator("+|-|*|/");

class ArithOperatorInt("%|<<|>>");

class LogicalOperator("&&|[|][|]");

class BitwiseOperator("&|[|]|^");

class RelationalOperator("==|!=|>|<|>=|<=");

class IncDecOp("++|--");

@copy
@max_height(8)
class Expr {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh safe_only : boolean;

  grd possible; # early deconstruction
  grd type_matches;

  syn type : Type;

  @weight(1)
  atom ("${atom : ExprAtom}") {
    this.possible = true;
    this.type_matches = true;
  }

  @weight(13)
  arith_op ("(${lhs : Expr}) ${op : ArithOperator} (${rhs : Expr})") {
    this.possible =
      (or
        (not this.safe_only)
        (not (equals op.str "/"))
      );

    this.type_matches = (Type:isNumberType this.expected_type);
    this.type = (Type:fromBinaryOperator lhs.type rhs.type);
  }

  @weight(13)
  arith_op_int ("(${lhs : Expr}) ${op : ArithOperatorInt} (${rhs : Expr})") {
    this.possible =
        (and
          (Type:assignable (Type:intType false) this.expected_type)
          (not this.safe_only)
        );

    lhs.expected_type = (Type:intType false);
    rhs.expected_type = (Type:intType false);

    this.type_matches = true;
    this.type = lhs.type;
  }

  @weight(1)
  logical_op ("(${lhs : Expr}) ${op : LogicalOperator} (${rhs : Expr})") {
    this.possible = true;

    lhs.expected_type = (Type:intType false);
    rhs.expected_type = (Type:intType false);

    this.type_matches = (Type:assignable (Type:intType false) this.expected_type);
    this.type = (Type:intType false);
  }

  @weight(2)
  logical_not ("!(${val : Expr})") {
    this.possible = true;

    val.expected_type = (Type:intType false);

    this.type_matches = (Type:assignable (Type:intType false) this.expected_type);
    this.type = (Type:intType false);
  }

  @weight(4)
  bitwise_op ("(${lhs : Expr}) ${op : BitwiseOperator} (${rhs : Expr})") {
    this.possible = true;

    lhs.expected_type = (Type:intType false);
    rhs.expected_type = (Type:intType false);

    this.type_matches = (Type:assignable (Type:intType false) this.expected_type);
    this.type = (Type:intType false);
  }

  @weight(1)
  bitwise_not ("~(${val : Expr})") {
    this.possible = true;

    val.expected_type = (Type:intType false);

    this.type_matches = (Type:assignable (Type:intType false) this.expected_type);
    this.type = (Type:intType false);
  }

  @weight(13)
  relational_op ("(${lhs : Expr}) ${op : RelationalOperator} (${rhs : Expr})") {
    this.possible = true;

    lhs.expected_type = (Type:realType false);
    rhs.expected_type = (Type:realType false);

    this.type_matches = (Type:assignable (Type:intType false) this.expected_type);
    this.type = (Type:intType false);
  }

  @weight(12)
  conditional ("((${cond : Condition}) ? (${then : ConditionalHelper}) : (${else : Expr}))") {
    this.possible = true;

    else.expected_type = then.type;

    this.type_matches = true;
    this.type = then.type;
  }

  @weight(1)
  pre_op ("${op : IncDecOp}(${lval : LValue})") {
    this.possible =
      (and
        (Type:isNumberType this.expected_type)
        (not this.safe_only)
      );

    lval.const_allowed = false;
    lval.bitfield_allowed = true;

    this.type_matches = (Type:assignable lval.type this.expected_type);

    this.type = lval.type;
  }

  @weight(1)
  post_op ("(${lval : LValue})${op : IncDecOp}") {
    this.possible =
      (and
        (Type:isNumberType this.expected_type)
        (not this.safe_only)
      );

    lval.const_allowed = false;
    lval.bitfield_allowed = true;

    this.type_matches = (Type:assignable lval.type this.expected_type);

    this.type = lval.type;
  }

}

class ConditionalHelper {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh safe_only : boolean;

  syn type : Type;

  @copy
  conditional_helper ("${expr : Expr}") {
    # intentionally left blank
  }

}

@copy
class ExprAtom {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh safe_only : boolean;

  grd possible; # early deconstruction
  grd type_matches;

  syn type : Type;

  @weight(2)
  num ("${val : Number}") {
    # no need to check safe_only
    this.possible = (Type:isNumberType this.expected_type);

    this.type_matches = true;
    this.type = val.type;
  }

  compound_literal ("${lit : CompoundLiteral}") {
    this.possible =
      (and
        (not this.safe_only)
        (Type:isCompositeType this.expected_type)
      );

    this.type_matches = true;
    this.type = this.expected_type;
  }

  null_ptr ("((void *) 0)") {
    # no need to check safe_only
    this.possible = (Type:isPointerType this.expected_type);
    this.type_matches = (Type:isPointerType this.expected_type);
    this.type = this.expected_type;
  }

  void_ptr ("((void *) (${expr : Expr}))") {
    loc pointer_expected = (Type:isPointerType this.expected_type);

    # no need to check safe_only
    this.possible = .pointer_expected;
    this.type_matches = .pointer_expected;

    expr.expected_type = (Type:intType false);

    this.type = this.expected_type;
  }

  @weight(2)
  var ("${name : UseIdentifier}") {
    loc var_type = (Symbol:getType name.symbol);

    this.possible = (not this.safe_only);

    this.type_matches = (Type:assignable .var_type this.expected_type);
    
    this.type = .var_type;
  }

  array_load ("${access : ArrayAccess}") {
    this.possible = (not this.safe_only);

    access.const_allowed = true;

    this.type_matches = true; # type is checked in 'ArrayAccess'

    this.type = (Type:getArrayBaseType (Symbol:getType access.symbol));
  }

  member_access ("${access : MemberAccess}") {
    this.possible = (not this.safe_only);

    access.const_allowed = true;
    access.bitfield_allowed = true;

    this.type_matches = true; # type is checked in 'MemberAccess'

    this.type = (Symbol:getType access.symbol);
  }

  call ("${call : Call}") {
    this.possible = (not this.safe_only);
    this.type_matches = true; # type is checked in 'Call'
  }

  addr ("&(${lval : LValue})") {
    loc pointer_type = (Type:createPointerType lval.type false);

    this.possible =
      (and
        (Type:isPointerType this.expected_type)
        (not this.safe_only)
      );

    lval.expected_type = (Type:getPointeeType this.expected_type);

    lval.const_allowed = true;
    lval.bitfield_allowed = false;

    this.type_matches = true;

    this.type = .pointer_type;
  }

  deref ("*(${pointer : LValue})") {
    this.possible = (not this.safe_only);

    pointer.expected_type = (Type:createPointerType this.expected_type false);

    pointer.const_allowed = true;
    pointer.bitfield_allowed = true;

    this.type_matches = true;

    this.type = (Type:getPointeeType pointer.type);
  }

}

@copy
class CompoundLiteral {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh safe_only : boolean;

  grd allowed;

  struct_literal (
      "((struct #{(Type:getName this.expected_type)}) {${members : CompoundLiteralMembers}})") {
    this.allowed = (Type:isStructType this.expected_type);

    members.expected_type = (Type:toTupleType this.expected_type);
  }

  union_literal (
      "((union #{(Type:getName this.expected_type)}) {${members : CompoundLiteralMembers}})") {
    this.allowed = (Type:isUnionType this.expected_type);

    members.expected_type = (Type:toTupleType this.expected_type);
  }

}

@copy
@list
class CompoundLiteralMembers {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh safe_only : boolean;

  grd allowed;

  one_member ("${val : Expr}") {
    this.allowed = (== (Type:getTupleTypeSize this.expected_type) 1);

    val.expected_type = (Type:getTupleTypeHead this.expected_type);
  }

  mult_members ("${val : Expr}, ${rest : CompoundLiteralMembers}") {
    this.allowed = (> (Type:getTupleTypeSize this.expected_type) 1);

    val.expected_type = (Type:getTupleTypeHead this.expected_type);
    rest.expected_type = (Type:getTupleTypeTail this.expected_type);
  }

}

class ArrayAccess {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh const_allowed : boolean;

  syn symbol : Symbol;

  grd valid;
  grd const_matches;

  @copy
  array_access ("${array : UseIdentifier}${indexes : IndexList}") {
    loc array_type = (Symbol:getType array.symbol);
    loc array_base_type = (Type:getArrayBaseType .array_type);

    array.expected_type = nil;

    indexes.expected_type = .array_type;

    this.symbol = array.symbol;

    this.valid =
      (and
        (Type:isArrayType .array_type)
        (Type:assignable .array_base_type this.expected_type)
      );

    this.const_matches =
      (or
        this.const_allowed
        (not (Type:isConst .array_base_type))
      );
  }

}

@copy
@list
class IndexList {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd valid;

  one_index ("[${index : Expr}]") {
    index.safe_only = false;

    index.expected_type = (Type:intType false);
    this.valid = (== (Type:getArrayDimensionality this.expected_type) 1);
  }

  mult_index ("[${index : Expr}]${rest : IndexList}") {
    index.safe_only = false;

    index.expected_type = (Type:intType false);
    rest.expected_type = (Type:narrowArrayType this.expected_type);

    this.valid = (> (Type:getArrayDimensionality this.expected_type) 1);
  }

}

class MemberAccess {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;
  inh const_allowed : boolean;
  inh bitfield_allowed : boolean;
  
  syn symbol : Symbol;

  grd valid;
  grd const_matches;

  @copy
  member_access ("(${composite : UseComposite}).${member : UseIdentifier}") {
    loc member_type = (Symbol:getType member.symbol);

    composite.expected_member_type = this.expected_type;

    member.symbols_before = (Type:getMembers composite.type);

    this.valid =
      (and
        (Type:assignable .member_type this.expected_type)
        (Type:bitFieldAllowed .member_type this.bitfield_allowed)
      );

    this.symbol = member.symbol;

    this.const_matches =
      (or
        this.const_allowed
        (and
          (not (Type:isConst composite.type))
          (not (Type:isConst .member_type))
        )
      );
  }

}

@max_height(6)
class UseComposite {

  inh expected_member_type : Type;
  inh symbols_before : SymbolTable;

  syn type : Type;

  @copy
  use_composite ("${composite : ExprAtom}") {
    composite.expected_type = (Type:anyComposite this.expected_member_type);
    composite.safe_only = false;
  }

}

@copy
class Call {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd valid;

  syn type : Type;

  call_no_args ("${callee : Callee}()") {
    loc callee_type = (Symbol:getType callee.symbol);

    this.valid = (== (Type:getTupleTypeSize (Type:getParameterType .callee_type)) 0);

    callee.expected_return_type = this.expected_type;

    this.type = (Type:getReturnType .callee_type);
  }

  call_args ("${callee : Callee}(${args : ArgumentList})") {
    loc callee_type = (Symbol:getType callee.symbol);

    this.valid = (> (Type:getTupleTypeSize (Type:getParameterType .callee_type)) 0);
    args.expected_type = (Type:getParameterType .callee_type);

    callee.expected_return_type = this.expected_type;

    this.type = (Type:getReturnType .callee_type);
  }

}

class Callee {

  inh symbols_before : SymbolTable;
  inh expected_return_type : Type;

  syn symbol : Symbol;

  grd valid;
  grd type_matches;

  @copy
  callee ("${func : UseIdentifier}") {
    loc func_type = (Symbol:getType func.symbol);

    func.expected_type = nil;

    this.symbol = func.symbol;

    this.valid = (Type:isFunctionType .func_type);

    this.type_matches =
      (Type:assignable (Type:getReturnType .func_type) this.expected_return_type);
  }

}

@copy
@list
class ArgumentList {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  grd valid;

  one_arg ("${val : Expr}") {
    val.expected_type = (Type:getTupleTypeHead this.expected_type);
    val.safe_only = false;

    this.valid = (== (Type:getTupleTypeSize this.expected_type) 1);
  }

  mult_arg ("${val : Expr}, ${rest : ArgumentList}") {
    val.expected_type = (Type:getTupleTypeHead this.expected_type);
    rest.expected_type = (Type:getTupleTypeTail this.expected_type);

    val.safe_only = false;

    this.valid = (> (Type:getTupleTypeSize this.expected_type) 1);
  }

}

@count(1000)
class IntNumber("0|[1-9][0-9]{0,6}");

@count(25)
class SmallIntNumber("1|2|3|4|5|6|7|8|9|[1-9][0-9]");

@count(1000)
class FloatNumber("[0-9]{1,4}.[0-9]{1,4}");

class Number {

  inh expected_type : Type;
  syn type : Type;

  grd type_matches;

  int_number ("${num : IntNumber}") {
    loc type = (Type:intType true);
    this.type_matches = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

  float_number ("${num : FloatNumber}") {
    loc type = (Type:realType true);
    this.type_matches = (Type:assignable .type this.expected_type);
    this.type = .type;
  }

}

@count(1000)
class Identifier("[a-z][a-zA-Z0-9_]{2,5}");

class DefIdentifier {

  inh symbols_before : SymbolTable;

  syn name : String;

  grd name_unique;

  def_id ("${id : Identifier}") {
    this.name = id.str;
    this.name_unique = (SymbolTable:mayDefine this.symbols_before id.str);
  }

}

class UseIdentifier {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn symbol : Symbol;

  use_id (SymbolTable:visibleIdentifiers this.symbols_before this.expected_type) : String {
    this.symbol = (SymbolTable:get this.symbols_before $);
  }

}

class PrimitiveType {

  inh types_before : SymbolTable;
  inh const : boolean;

  syn type : Type;
  syn width : int;

  type_int8 ("int8_t") {
    this.type = (Type:intType this.const);
    this.width = 8;
  }

  type_int16 ("int16_t") {
    this.type = (Type:intType this.const);
    this.width = 16;
  }

  type_int32 ("int32_t") {
    this.type = (Type:intType this.const);
    this.width = 32;
  }

  type_uint8 ("uint8_t") {
    this.type = (Type:intType this.const);
    this.width = 8;
  }

  type_uint16 ("uint16_t") {
    this.type = (Type:intType this.const);
    this.width = 16;
  }

  type_uint32 ("uint32_t") {
    this.type = (Type:intType this.const);
    this.width = 32;
  }

  type_double ("double") {
    this.type = (Type:realType this.const);
    this.width = 64;
  }

}

class VolatileQualifier {

  inh qualifiers_allowed : boolean;

  grd valid;

  @weight(1)
  volatile ("volatile ") {
    this.valid = this.qualifiers_allowed;
  }

  @weight(5)
  not_volatile ("") {
    this.valid = true;
  }

}

class ConstQualifier {

  inh qualifiers_allowed : boolean;

  syn is_const : boolean;

  grd valid;

  @weight(1)
  const ("const ") {
    this.valid = this.qualifiers_allowed;
    this.is_const = true;
  }

  @weight(5)
  not_const ("") {
    this.valid = true;
    this.is_const = false;
  }

}

@copy
class AtomicType {

  inh types_before : SymbolTable;
  inh qualifiers_allowed : boolean;

  syn type : Type;

  @weight(7)
  type_primitive ("${cnst : ConstQualifier}${vol : VolatileQualifier}${type : PrimitiveType}") {
    type.const = cnst.is_const;
    this.type = type.type;
  }

  type_composite ("${vol : VolatileQualifier}${composite : CompositeType}") {
    this.type = composite.type;
  }

}

class CompositeType {

  inh types_before : SymbolTable;

  syn type : Type;

  grd valid;

  struct_type ("struct ${name : UseIdentifier}") {
    loc name_type = (Symbol:getType name.symbol);

    name.symbols_before = this.types_before; # sic
    name.expected_type = nil;

    this.valid = (Type:isStructType .name_type);
    this.type = .name_type;
  }

  union_type ("union ${name : UseIdentifier}") {
    loc name_type = (Symbol:getType name.symbol);

    name.symbols_before = this.types_before; # sic
    name.expected_type = nil;

    this.valid = (Type:isUnionType .name_type);
    this.type = .name_type;
  }

}

@copy
@list
class PointerType {

  inh types_before : SymbolTable;
  inh qualifiers_allowed : boolean;

  syn type : Type;

  @weight(3)
  one_pt ("${pointee : AtomicType} *${cnst : ConstQualifier}${vol : VolatileQualifier}") {
    cnst.qualifiers_allowed = true;
    vol.qualifiers_allowed = true;

    this.type = (Type:createPointerType pointee.type cnst.is_const);
  }

  mult_pt ("${pointee : PointerType}*${cnst : ConstQualifier}${vol : VolatileQualifier}") {
    cnst.qualifiers_allowed = true;
    vol.qualifiers_allowed = true;

    this.type = (Type:createPointerType pointee.type cnst.is_const);
  }

}

@copy
class Type {

  syn type : Type;

  inh types_before : SymbolTable;

  @weight(3)
  atomic_type ("${type : AtomicType}") {
    type.qualifiers_allowed = true;
  }

  pointer_type ("${type : PointerType}") {
    type.qualifiers_allowed = true;
  }

}

@list(4)
class ArrayDimensions {

  inh base_type : Type;

  syn type : Type;

  one_dim ("[${size : SmallIntNumber}]") {
    this.type = (Type:createOneDimensionalArrayType this.base_type);
  }

  @copy
  mult_dim ("${other_dims : ArrayDimensions}[${size : SmallIntNumber}]") {
    this.type = (Type:extendArrayType other_dims.type);
  }

}

@copy
class ReturnType {

  syn type : Type;

  inh types_before : SymbolTable;

  primitive_type ("${type : PrimitiveType}") {
    type.const = false;
  }

  pointer_type ("${type : PointerType}") {
    type.qualifiers_allowed = false;
  }

  void_type ("void") {
    this.type = (Type:voidType);
  }

}
