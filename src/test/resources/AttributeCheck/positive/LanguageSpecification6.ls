class A {

  p ("${b1 : B}${b2 : B}") {
    b1.a = 13;
    b2.a = 13;
  }

}

class B {

  inh a : int;

  b ("") {}

}
