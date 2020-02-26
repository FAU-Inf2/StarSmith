#!/bin/bash

javac -cp "$(dirname "$0")/../../../build/libs/StarSmith.jar":./ *.java
