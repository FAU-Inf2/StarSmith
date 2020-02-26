class A {

  syn s1 : int;

  p_A ("${b : B}") {
    this.s1 = b.i;
    b.i = 13;
  }

}

class B {

  inh i : int;

  p_B ("") { }

}
