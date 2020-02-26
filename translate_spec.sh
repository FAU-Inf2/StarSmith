#!/bin/bash

java -Xss2m -ea -jar $(dirname $0)/build/libs/StarSmith.jar "$@"
