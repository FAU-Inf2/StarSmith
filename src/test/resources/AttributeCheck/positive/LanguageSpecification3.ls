class A {

  p ("${b : B}") {}

}

class B {

  syn a : int;

  b ("") {
    this.a = 1303;
  }
}
