#!/bin/bash

set -e
LC_NUMERIC="C.UTF-8"

COLOR_YELLOW="\033[1;33m"
COLOR_BLUE="\033[1;36m"
COLOR_NONE="\033[0m"

function print_step {
  printf "${COLOR_YELLOW}======[ ${1} ]======${COLOR_NONE}\n"
}

compile_args=$@

out_dirs=('c' 'lua' 'smt' 'sql' 'sql' 'paper_spec' 'paper_spec')
specs=('c' 'lua' 'smt' 'sql_wrapper' 'sql_arith' 'paper_spec' 'paper_spec_generators')
max_depths=('11' '13' '11' '40' '40' '11' '11')


# == TRANSLATE RUNTIME CLASSES
print_step "translate runtime classes"
for i in $(echo ${!out_dirs[@]} | tr ' ' '\n' | sort -u | tr '\n' ' ') ; do
  out_dir="out/${out_dirs[$i]}"
  pushd "$out_dir/runtime" > /dev/null ; ./compile_all.sh ; popd > /dev/null
done


# == TRANSLATE SPECIFICATIONS
print_step "translate specifications"
for i in ${!specs[@]} ; do
  spec_file="specs/${specs[$i]}.ls"
  out_dir="out/${out_dirs[$i]}"
  java_file="$out_dir/${specs[$i]}.java"
  max_depth="${max_depths[$i]}"

  echo "- $spec_file => $java_file"

  ./translate_spec.sh --spec "$spec_file" --maxDepth "$max_depth" --allFeatures --toJava "$java_file" $compile_args
  pushd "$out_dir" > /dev/null ; ./compile.sh "$(basename $java_file)" ; popd > /dev/null
done
