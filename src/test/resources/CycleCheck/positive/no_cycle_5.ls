class A {

  p_A ("${b : B}") {
    b.b1 = b.b2;
  }

}

class B {

  inh b1 : int;
  syn b2 : int;

  p_B ("${c1 : C}${c2 : C}") {
    c1.c1 = this.b1;
    c2.c1 = 13;
    this.b2 = c2.c2;
  }

}

class C {

  inh c1 : int;
  syn c2 : int;

  p_C ("") {
    this.c2 = this.c1;
  }

}
