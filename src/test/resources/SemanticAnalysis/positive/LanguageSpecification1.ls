class A {

  syn a : int;

  p ("${b : B}") {
    b.a1 = 13;
    this.a = b.a2;
  }

}

class B {

  inh a1 : int;
  syn a2 : int;

  p ("") {
    this.a2 = this.a1;
  }

}
