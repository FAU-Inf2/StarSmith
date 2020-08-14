# StarSmith

This repository accompanies the research paper "P. Kreutzer, S. Kraus, M. Philippsen:
Language-Agnostic Generation of Compilable Test Programs" published at ICST'20 ([link to
publication](https://ieeexplore.ieee.org/document/9159098)).

Please follow the instructions below to build and execute StarSmith. The implementation was tested
on Debian 8 running OpenJDK 8 (and the instructions assume that you use a comparable system).

## Directory Structure

These are the most important sub-directories:

- `src/`: contains the StarSmith implementation
- `specs/`: contains the LaLa specifications
- `out/`: contains the runtime classes for each specification as well as several helper scripts to
  translate and execute the generated Java code (see below)

## Building StarSmith

Simply run `./gradlew build` to build StarSmith. After a successful build, there should be a file
`build/libs/StarSmith.jar`. The instructions below assume that this file exists.

**Note**:

- You need a working JDK installation to build and run StarSmith (tested with OpenJDK 8).
- Building StarSmith requires an internet connection to resolve external dependencies.

## Manual Translation and Execution of a LaLa Specification

The following instructions describe how to *manually* translate and execute a LaLa specification to
generate random programs (the examples below show this for the C specification, but the steps for
the other specifications are the same).

**Note**: For your convenience, we also provide helper scripts that *automatically* translate *all*
specifications (see below). We highly recommend that you use these scripts instead of manually
translating the specifications and generated Java sources.

From a technical point of view, the workflow is as follows:

```
+---------------+               +--------+            +---------+
|     LaLa      |   translate   |  Java  |   compile  |  Java   |  run
| Specification |  ==========>  | source |  ========> | classes | =====>
+---------------+               +--------+            +---------+   ||
                                                                    ||
+---------+           +---------+                                   ||
| runtime |  compile  | runtime |                                   ||
| source  | ========> | classes |  ===================================
+---------+           +---------+
```

The following steps are required to generate random programs from a LaLa specification:

1. The first step is to *compile* the *runtime sources* for the given specification:
    - Example: `cd out/c/runtime ; ./compile_all.sh`
    - This step is only necessary once (and after a change to the runtime sources).

2. The next step is to *translate* the *language specification* to Java source code using the
   `translate_spec.sh` script:
    - Example: `./translate_spec.sh --spec specs/c.ls --maxDepth 11 --toJava out/c/c.java`
    - **Important**: The `--maxDepth` option determines the height limit for recursive productions.
      Use the following values (if the value is too low, the program generation will fail with a
      runtime error!):
        - C: 11
        - Lua: 13
        - SMT: 11
        - SQL: 40
        - PaperSpec: 11
    - This step is only necessary once (and after a change to the LaLa specification).

3. *Compile* the *Java sources* generated from the LaLa specification:
    - Example: `cd out/c ; ./compile.sh c.java`
    - This step is only necessary once (and after a change to the LaLa specification).

4. You can now *run* the generated *Java classes* to generate random programs:
    - Example: `cd out/c ; ./run.sh c` (note the `c` argument for `run.sh`!)
    - If no further arguments are provided, StarSmith generates a single random program and prints
      it to stdout.

If you want to generate multiple programs and write them to disk, use the following (**note**: there
are also helper scripts that generate multiple programs, see below):

```
cd out/c
mkdir output
./run.sh c \
  --seed 0 --count 1000 \
  --out output/batch_#{BATCH}/prog_#{SEED}.c \
  --batchSize 100 \
  --stats output/stats.csv
```

This uses the following command line options:

- `--seed`: Specifies the random seed for the generation of the first program.
- `--count`: Specifies the number of programs that should be generated. Use a count of `INF` to
  generate infinitely many programs.
- `--out`: Specifies the file name pattern for the generated programs. It may use the following
  placeholders that will be replaced for each program:
  - `#{BATCH}`: You can split the generated programs in batches to ease the handling of the
    generated files (also see `--batchSize` below). The placeholder will be replaced by the number
    of the batch that the generated program belongs to.
  - `#{SEED}`: Will be replaced by the random seed that was used for the generated program.
- `--batchSize`: Specifies the number of programs per batch.
- `--stats`: Generates a CSV file containing statistics about the generated programs. It contains
  the following columns:
  - Number of AST nodes
  - Generation time (in ms)
  - AST height


## Automatically Translating all LaLa Specifications (and Runtime Classes)

Use the following helper script to automatically translate all LaLa specifications and their
corresponding runtime classes:

```
./translate_all.sh
```

Once all specifications have been translated, you can use the following script to generate ten
example programs per specification:

```
./generate_examples.sh
```

See the files in the sub-directory `examples` for the generated example programs.


## Generating Random Programs

**Important**: The following instructions assume that the LaLa specifications have already been
translated and compiled (see above).

For each specification, there are helper scripts to generate multiple programs (including scripts to
generate programs for 72 hours as we did for our evaluation in the paper).

In the following, we show how to use these scripts for the C case study. There are respective
scripts for the other case studies as well (which are located in the same places within their
corresponding `out` sub-directory).

To generate C programs for 72 hours, follow these instructions:

```
cd out/c/evaluation
./generate_72h.sh run_72h/
```

This will generate random programs for 72 hours and write them to the `run_72h` sub-directory. This
directory also contains a `stats.csv` file with statistics about the generated programs (see above).

**Warning**: This requires a lot of free disk space (and takes... 72 hours)!

There is also a helper script to generate a smaller subset of random programs:

```
cd out/c/evaluation
./generate_sample.sh run_sample/ 1000
```

The second argument for the `generate_sample.sh` script determines the number of generated programs.

If you only want to generate a single program with a fixed random seed (`1303` in the example),
execute the following:

```
cd out/c/evaluation
./generate_single.sh run_single/ 1303
```

Note that we provide different scripts for the two SQL specifications.

## TODO: Documentation

Currently, there is no real documentation for LaLa and StarSmith. If you want to write your own
specification, please take a look at the example specifications (and runtime classes) that we
provide. 

## License

StarSmith is licensed under the terms of the MIT license (see [LICENSE.mit](LICENSE.mit)).

StarSmith makes use of the following open-source projects:

- ANTLR (licensed under the terms of The BSD License)
- JUnit (licensed under the terms of Eclipse Public License)
- Jackson (licensed under the terms of Apache License 2.0)
- Gradle (licensed under the terms of Apache License 2.0)
