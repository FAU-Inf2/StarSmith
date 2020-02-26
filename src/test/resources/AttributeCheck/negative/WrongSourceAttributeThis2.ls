class A {

  syn s1 : int;

  p_A ("${b : B}") {
    this.s1 = 13;
    b.i = this.s1;
  }

}

class B {

  inh i : int;

  p_B ("") { }

}
