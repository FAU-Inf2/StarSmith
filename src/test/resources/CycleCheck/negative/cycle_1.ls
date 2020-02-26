class A {

  p_A ("${b : B}") {
    b.b1 = b.b2;
  }

}

class B {

  inh b1 : int;
  syn b2 : int;

  p_B ("${c : C}${d : D}") {
    c.c1 = this.b1;
    d.d1 = c.c2;
    this.b2 = d.d2;
  }

}

class C {

  inh c1 : int;
  syn c2 : int;

  p_C ("") {
    this.c2 = this.c1;
  }

}

class D {

  inh d1 : int;
  syn d2 : int;

  p_C ("") {
    this.d2 = this.d1;
  }

}
