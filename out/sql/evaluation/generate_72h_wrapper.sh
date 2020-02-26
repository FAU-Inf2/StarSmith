#!/bin/bash

set -e

if [ $# -ne 1 ] ; then
  echo "USAGE: $0 <output directory>"
  exit 1
fi

out_dir="$1"
mkdir -p "$out_dir"

timeout 72h "$(dirname "$0")/../run.sh" sql_wrapper --seed 0 --count INF \
  --out "${out_dir}/batch_#{BATCH}/query_#{SEED}.sql" \
  --batchSize 100000 \
  --stats "${out_dir}/stats.csv"
