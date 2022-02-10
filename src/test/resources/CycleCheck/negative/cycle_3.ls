class Root {

  r ("${med : Med}") {
    med.i = med.s;
  }

}

class Med {

  inh i : int;
  syn s : int;

  m ("${child : Child}") {
    child.i1 = this.i;
    child.i2 = child.s1;
    this.s = child.s2;
  }

}

class Child {

  inh i1 : int; inh i2 : int;
  syn s1 : int; syn s2 : int;

  c1 ("#{this.i1}#{this.i2}") {
    this.s1 = this.i1;
    this.s2 = 0;
  }

  c2 ("#{this.i1}#{this.i2}") {
    this.s2 = this.i2;
    this.s1 = 0;
  }

}
