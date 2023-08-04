#!/usr/bin/env bash

# Needed to compile bpc.bps

set -euo pipefail

# get script directory
SOURCE=${BASH_SOURCE[0]}
DIR=$( dirname "$SOURCE" )

PROJECT_PATH="$DIR/.."
export JAR_PATH="${PROJECT_PATH}/target/bashpile-jar-with-dependencies.jar"
java -jar "$JAR_PATH" "$1"