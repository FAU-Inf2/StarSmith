class A {

  syn a1 : int;

  p_A ("${b : B}") {
    b.b1 = 13;
    this.a1 = b.b2;
  }

}

class B {

  inh b1 : int;
  syn b2 : int;

  p_B_1 ("") {
    this.b2 = 13;
  }

  p_B_2 ("${b : B}") {
    b.b1 = 13;
    this.b2 = b.b2;
  }

}
