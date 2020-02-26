#!/bin/bash

if [ $# -ne 1 ] ; then
  echo "USAGE: $0 <Java file>"
  exit 1
fi

this_dir="$(dirname "$0")"
class_dir="$this_dir/classes/"

mkdir -p "$class_dir"
javac -cp "$this_dir/../../build/libs/StarSmith.jar":"$this_dir" -d "$class_dir" "$1"
