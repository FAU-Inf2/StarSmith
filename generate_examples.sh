#!/bin/bash

set -e

readonly COUNT=10

out_dir="$(dirname "$0")/examples/"
mkdir -p "$out_dir"

COLOR_YELLOW="\033[1;33m"
COLOR_BLUE="\033[1;36m"
COLOR_NONE="\033[0m"

function print_step {
  printf "${COLOR_YELLOW}======[ ${1} ]======${COLOR_NONE}\n"
}

print_step "PaperSpec (without generators)"
"$(dirname "$0")/out/paper_spec/run.sh" paper_spec --seed 0 --count "$COUNT" \
  --out "$out_dir/paper_spec/program_#{SEED}.txt"

print_step "PaperSpec (with generators)"
"$(dirname "$0")/out/paper_spec/run.sh" paper_spec_gen --seed 0 --count "$COUNT" \
  --out "$out_dir/paper_spec_gen/program_#{SEED}.txt"

print_step "C"
"$(dirname "$0")/out/c/run.sh" c --seed 0 --count "$COUNT" \
  --out "$out_dir/c/prog_#{SEED}.c"

print_step "Lua"
"$(dirname "$0")/out/lua/run.sh" lua --seed 0 --count "$COUNT" \
  --out "$out_dir/lua/prog_#{SEED}.lua"

print_step "SMT"
"$(dirname "$0")/out/smt/run.sh" smt --seed 0 --count "$COUNT" \
  --out "$out_dir/smt/q_#{SEED}.smt2"

print_step "SMT (FP)"
"$(dirname "$0")/out/smt/run.sh" smt_fp --seed 0 --count "$COUNT" \
  --out "$out_dir/smt_fp/q_#{SEED}.smt2"

print_step "SMT (strings)"
"$(dirname "$0")/out/smt/run.sh" smt_strings --seed 0 --count "$COUNT" \
  --out "$out_dir/smt_strings/q_#{SEED}.smt2"

print_step "SMT (strings, subset)"
"$(dirname "$0")/out/smt/run.sh" smt_strings_cvc4_1_8 --seed 0 --count "$COUNT" \
  --out "$out_dir/smt_strings_cvc4_1_8/q_#{SEED}.smt2"

print_step "SQL (wrapper functions)"
"$(dirname "$0")/out/sql/run.sh" sql_wrapper --seed 0 --count "$COUNT" \
  --out "$out_dir/sql_wrapper/query_#{SEED}.sql"

print_step "SQL (arithmetic operations)"
"$(dirname "$0")/out/sql/run.sh" sql_arith --seed 0 --count "$COUNT" \
  --out "$out_dir/sql_arith/query_#{SEED}.sql"
