#!/bin/bash

set -e

if [ $# -ne 2 ] ; then
  echo "USAGE: $0 <output directory> <sample size>"
  exit 1
fi

out_dir="$1"
mkdir -p "$out_dir"

sample_size="$2"

"$(dirname "$0")/../run.sh" smt --seed 0 --count "$sample_size" \
  --out "${out_dir}/batch_#{BATCH}/q_#{SEED}.smt2" \
  --batchSize 100000 \
  --stats "${out_dir}/stats.csv"
