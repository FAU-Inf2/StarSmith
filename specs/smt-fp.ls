use BitVector;
use Symbol;
use SymbolTable;
use Type;

class Script {

  script (
      "(set-logic QF_FP)\n
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
  declare_fun
      ("(declare-fun ${def : DefIdentifier} () ${ret : Sort})") {
    this.possible = true;
    this.symbols_after =
      (SymbolTable:put
        this.symbols_before
        (Symbol:create def.name (Type:createFunctionType ret.type (Type:createEmptyTupleType))));
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

  @weight(1)
  primitive_sort ("${sort : PrimitiveSort}") {
    this.type = sort.type;
  }

  @weight(10)
  floating_sort ("${sort : FloatingPointSort}") {
    this.type = sort.type;
  }

}

class PrimitiveSort {

  syn type : Type;

  bool ("Bool") {
    this.type = (Type:boolSort);
  }

}

@count(100)
class FPConstant("[1-3][0-9]{0,1}");

class FloatingPointSort {

  syn type : Type;

  grd allowed;

  @weight(1)
  floating_sort ("(_ FloatingPoint ${expBits : FPConstant} ${sigfBits : FPConstant})") {
    this.allowed = (and (> (int expBits.str) 1) (> (int sigfBits.str) 1));
    this.type = (Type:createFloatingPointSort (int expBits.str) (int sigfBits.str));
  }

  @weight(1)
  float16 ("Float16") {
    this.allowed = true;
    this.type = (Type:createFloatingPointSort 5 11);
  }

  @weight(10)
  float32 ("Float32") {
    this.allowed = true;
    this.type = (Type:createFloatingPointSort 8 24);
  }

  @weight(10)
  float64 ("Float64") {
    this.allowed = true;
    this.type = (Type:createFloatingPointSort 11 53);
  }

  @weight(1)
  float128 ("Float128") {
    this.allowed = true;
    this.type = (Type:createFloatingPointSort 15 113);
  }
}

@copy(symbols_before, expected_type)
class Expression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  grd type_matches;

  @weight(1)
  _true ("true") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);
    this.type = (Type:boolSort);
  }

  @weight(1)
  _false ("false") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);
    this.type = (Type:boolSort);
  }

  @weight(100)
  var ("${id : UseIdentifier}") {
    this.type_matches = (Type:assignable (Symbol:getType id.symbol) this.expected_type);
    this.type = (Symbol:getType id.symbol);
  }

  @weight(20)
  fun_app ("${fun_app : FunApp}") {
    this.type_matches = true; # checked in FunApp
    this.type = fun_app.type;
  }

  @weight(5)
  @copy(type)
  core_expr ("${expr : Core_Expression}") {
    this.type_matches = (Type:isPrimitiveSort this.expected_type);
  }

  @weight(5)
  @copy(type)
  let ("${let : LetExpression}") {
    this.type_matches = true;
  }

  @weight(10)
  @copy(type)
  fp_expression ("${fp_expression : FPExpression}") {
    this.type_matches = true;
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

class FPUnaryNoRoundingFunction("fp.abs|fp.neg");
class FPUnaryRoundingFunction("fp.sqrt|fp.roundToIntegral");
class FPBinaryNoRoundingFunction("fp.rem|fp.min|fp.max");
class FPBinaryRoundingFunction("fp.add|fp.sub|fp.mul|fp.div");

class BoolFPUnaryFunction("fp.isNormal|fp.isSubnormal|fp.isZero|fp.isInfinite|fp.isNaN|fp.isNegative|fp.isPositive");
class BoolFPBinaryFunction("fp.eq|fp.leq|fp.lt|fp.geq|fp.gt");

@copy(symbols_before, expected_type)
class FPExpression {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  grd allowed;

  @weight(10)
  fp_literal ("${lit : FPLiteral}") {
    this.allowed = (Type:isFloatingPointSort this.expected_type);
    lit.min_exp_width = (Type:getMinExponentWidth this.expected_type);
    lit.max_exp_width = (Type:getMaxExponentWidth this.expected_type);
    lit.min_sigf_width = (Type:getMinSignificantWidth this.expected_type);
    lit.max_sigf_width = (Type:getMaxSignificantWidth this.expected_type);
    this.type = (Type:createFloatingPointSort lit.exp_width lit.sigf_width);
  }

  @weight(3)
  fp_unary_no_rnd_function ("(${func : FPUnaryNoRoundingFunction} ${op : Expression})") {
    this.allowed = (Type:isFloatingPointSort this.expected_type);
    this.type = op.type;
  }

  @weight(1)
  fp_unary_rnd_function
      ("(${func : FPUnaryRoundingFunction} ${rndMode : FPRoundingMode} ${op : Expression})") {
    this.allowed = (Type:isFloatingPointSort this.expected_type);
    this.type = op.type;
  }

  @weight(2)
  fp_binary_no_rnd_function
      ("(${func : FPBinaryNoRoundingFunction} ${op1 : FPExpressionHelper} ${op2 : Expression})") {
    this.allowed = (Type:isFloatingPointSort this.expected_type);
    op2.expected_type = op1.type;
    this.type = op1.type;
  }

  @weight(2)
  fp_binary_rnd_function
      ("(${func : FPBinaryRoundingFunction} ${rndMode : FPRoundingMode} ${op1 : FPExpressionHelper} ${op2 : Expression})") {
    this.allowed = (Type:isFloatingPointSort this.expected_type);
    op2.expected_type = op1.type;
    this.type = op1.type;
  }

  @weight(1)
  bool_fp_unary_function ("(${func : BoolFPUnaryFunction} ${op : Expression})") {
    this.allowed = (Type:assignable (Type:boolSort) this.expected_type);
    op.expected_type = (Type:anyFloatingPoint);
    this.type = (Type:boolSort);
  }

  @weight(2)
  bool_fp_binary_function (
      "(${func : BoolFPBinaryFunction} ${op : Expression} ${r : ExpressionChain})") {
    this.allowed = (Type:assignable (Type:boolSort) this.expected_type);

    op.expected_type = (Type:anyFloatingPoint);
    r.expected_type = op.type;
    r.min_length = 1;

    this.type = (Type:boolSort);
  }

  @weight(10)
  conv("((_ to_fp #{.eb} #{.sb}) ${rndMode : FPRoundingMode} ${op : Expression})") {
    loc eb = (Type:randomFPWidth
        this
        (Type:getMinExponentWidth this.expected_type)
        (Type:getMaxExponentWidth this.expected_type));
    loc sb = (Type:randomFPWidth
        this
        (Type:getMinSignificantWidth this.expected_type)
        (Type:getMaxSignificantWidth this.expected_type));

    this.allowed = (Type:isFloatingPointSort this.expected_type);

    op.expected_type = (Type:anyFloatingPoint);

    this.type = (Type:createFloatingPointSort .eb .sb);
  }
}

class FPRoundingMode ("roundNearestTiesToEven|RNE|roundNearestTiesToAway|RNA|roundTowardPositive|RTP|roundTowardNegative|RTN|roundTowardZero|RTZ");

class FPExpressionHelper {

  inh symbols_before : SymbolTable;
  inh expected_type : Type;

  syn type : Type;

  @copy
  helper ("${expr : Expression}") {
    # intentionally left blank
  }

}

class Sign ("+|-");

class FPLiteralPart {

  inh min_width : int;
  inh max_width : int;

  syn width : int;

  part ("#{.res}") {
    loc width = (Type:randomFPWidth this this.min_width this.max_width);
    # Note that "this .width" is interpreted as this.width. Use (+ .width 0) to avoid that.
    loc res = (BitVector:randomBitVector this (+ .width 0) .width);

    this.width = .width;
  }
}

class FPLiteral {

  inh min_exp_width : int;
  inh max_exp_width : int;
  inh min_sigf_width : int;
  inh max_sigf_width : int;

  syn exp_width : int;
  syn sigf_width : int;

  @weight(5)
  from_bv_lit ("(fp #b#{.sign} #b${exp : FPLiteralPart} #b${sigf : FPLiteralPart})") {
    loc sign = (BitVector:randomBitVector this 1 1);

    exp.min_width = this.min_exp_width;
    exp.max_width = this.max_exp_width;
    sigf.min_width = (if (== this.min_sigf_width -1) -1 (- this.min_sigf_width 1));
    sigf.max_width = (if (== this.max_sigf_width -1) -1 (- this.max_sigf_width 1));

    this.exp_width = exp.width;
    this.sigf_width = (+ sigf.width 1);
  }

  @weight(2)
  infinity ("(_ ${sign : Sign}oo #{.eb} #{.sb})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }

  @weight(5)
  zero ("(_ ${sign : Sign}zero #{.eb} #{.sb})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }

  @weight(1)
  nan ("(_ NaN #{.eb} #{.sb})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }

  @weight(1)
  from_real ("((_ to_fp #{.eb} #{.sb}) ${rndMode : FPRoundingMode} ${val : RealLiteral})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }

  @weight(2)
  from_signed_bv ("((_ to_fp #{.eb} #{.sb}) ${rndMode : FPRoundingMode} #b#{.bvval})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);
    loc bvval = (BitVector:randomBitVector this (+ .eb .sb) (+ .eb .sb));

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }

  @weight(2)
  from_unsigned_bv ("((_ to_fp_unsigned #{.eb} #{.sb}) ${rndMode : FPRoundingMode} #b#{.bvval})") {
    loc eb = (Type:randomFPWidth this this.min_exp_width this.max_exp_width);
    loc sb = (Type:randomFPWidth this this.min_sigf_width this.max_sigf_width);
    loc bvval = (BitVector:randomBitVector this (+ .eb .sb) (+ .eb .sb));

    this.exp_width = .eb;
    this.sigf_width = .sb;
  }
}

class Zeros("[0]{0,6}");

class RealLiteral {
  real ("${head : FPConstant}.${zeros : Zeros}${tail : FPConstant}") {
    # Intentionally left blank
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

  @weight(3)
  not ("(not ${expr : Expression})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    expr.expected_type = (Type:boolSort);

    this.type = (Type:boolSort);
  }

  @weight(3)
  bool_op ("(${op : BoolOperator} ${es : ExpressionChain})") {
    this.type_matches = (Type:assignable (Type:boolSort) this.expected_type);

    es.expected_type = (Type:boolSort);
    es.min_length = 2;

    this.type = (Type:boolSort);
  }

  @weight(1)
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
