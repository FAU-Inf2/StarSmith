#!/bin/bash

set -e

if [ $# -ne 2 ] ; then
  echo "USAGE: $0 <output directory> <seed>"
  exit 1
fi

out_dir="$1"
mkdir -p "$out_dir"

seed="$2"
batch=$((seed / 100000))

"$(dirname "$0")/../run.sh" sql_arith --seed "$seed" --count 1\
  --out "${out_dir}/batch_${batch}/query_#{SEED}.sql"
