class A {

  p_A ("${b : B}") {
    b.i1 = 13;
    b.i2 = b.i1;
  }

}

class B {

  inh i1 : int;
  inh i2 : int;

  p_B ("") { }

}
