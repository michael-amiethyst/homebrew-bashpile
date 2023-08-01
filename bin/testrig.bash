#!/usr/bin/env bash

# Usage: testrig com.bashpile.Bashpile program -gui src/test/resources/testrigData.bashpile

set -euo pipefail

mvn dependency:build-classpath -Dmdep.outputFile=cpath.txt
MAVEN_CLASSPATH=$(cat cpath.txt)
rm cpath.txt
java -cp target/classes:"$MAVEN_CLASSPATH" org.antlr.v4.gui.TestRig "$@"