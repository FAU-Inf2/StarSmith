class A {

  syn a : int;

  p ("${b : B}") {
    b.a = 13;
  }

}

class B {

  p ("") {}

}
