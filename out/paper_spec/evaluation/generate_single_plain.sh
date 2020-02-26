#!/bin/bash

set -e

if [ $# -ne 2 ] ; then
  echo "USAGE: $0 <output directory> <seed>"
  exit 1
fi

out_dir="$1"
mkdir -p "$out_dir"

seed="$2"

"$(dirname "$0")/../run.sh" paper_spec --seed "$seed" --count 1\
  --out "${out_dir}/prog_#{SEED}.txt"
