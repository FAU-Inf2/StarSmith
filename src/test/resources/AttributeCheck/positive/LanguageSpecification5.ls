class A {

  syn a1 : int;
  inh a2 : int;

  p ("${b : B}") {
    this.a1 = 13;
    b.a3 = 13;
  }

}

class B {

  inh a3 : int;
  syn a4 : int;

  p ("${a : A}") {
    this.a4 = 13;
    a.a2 = 13;
  }

}
