class A {

  p ("${b : B}") {
    b.a = 13;
  }

}

class B {

  syn a : int;

  b ("") {
    this.a = 1303;
  }

}
