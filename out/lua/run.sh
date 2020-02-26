#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "USAGE: $0 <class name> <program options>"
  exit 1
fi

this_dir="$(dirname "$0")"
class_dir="$this_dir/classes/"

time java -ea -cp "$this_dir/../../build/libs/StarSmith.jar":"$class_dir":"$this_dir" $@
