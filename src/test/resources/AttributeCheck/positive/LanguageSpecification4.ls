class A {

  p ("${b : B}") {
    b.a = 13;
  }

}

class B {

  inh a : int;

  b ("") {}

}
