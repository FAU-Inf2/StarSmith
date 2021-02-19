use SymbTab;

class Program {
  prog("${stmts : StmtList}\n") {
    stmts.symbs = (SymbTab:empty);
  }
}

@list
class StmtList {
  inh symbs : SymbTab;

  one("${s: Stmt}") {
    s.s_before = this.symbs;
  }
  @weight(3)
  mult("${s: Stmt}\n${r : StmtList}") {
    s.s_before = this.symbs;
    r.symbs = s.s_after;
  }
}

class Stmt {
  inh s_before : SymbTab;
  syn s_after : SymbTab;

  assign("${i : Identifier} := ${e : Expr};") {
    e.symbs = this.s_before;
    this.s_after = (SymbTab:put this.s_before i.str);
  }
}

class Expr {
  inh symbs : SymbTab;

  num("${val : Number}") {}
  use_var("${var : UseVariableGen}") {
    var.symbs = this.symbs;
  }
  binop("(${l : Expr}) ${op : Op} (${r : Expr})") {
    l.symbs = this.symbs;
    r.symbs = this.symbs;
  }
}

class UseVariableGen {
  inh symbs : SymbTab;

  var(SymbTab:defs this.symbs) : String { }
}

class Identifier("[a-z]{1,3}");
class Number("0|[1-9][0-9]{0,2}");
class Op("+|-|*|/");
