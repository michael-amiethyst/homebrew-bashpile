#!/usr/bin/env bash

set -euo pipefail

# get script directory
SOURCE=${BASH_SOURCE[0]}
DIR=$( dirname "$SOURCE" )

PROJECT_PATH="$DIR/.."
export JAR_PATH="${PROJECT_PATH}/target/bashpile-jar-with-dependencies.jar"
java -jar "$JAR_PATH" "$1" | tee output.txt
TRANSLATED_FILENAME=$(tail --lines 1 < output.txt)
shift
printf "Start of %s\n" "$TRANSLATED_FILENAME"
./"$TRANSLATED_FILENAME" "$@"
rm -f "$TRANSLATED_FILENAME" output.txt