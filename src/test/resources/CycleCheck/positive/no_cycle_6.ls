class Root {

  r ("${first : Child}${second : Child}") {
    second.i = first.s;
    first.i = 0;
  }

}

class Child {

  inh i : int;
  syn s : int;

  c ("#{this.i}") {
    this.s = this.i;
  }

}
