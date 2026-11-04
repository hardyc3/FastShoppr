#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

JAVAC="${JAVAC:-javac}"
JAVA="${JAVA:-java}"

out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

mapfile -t sources < <(find src/main/java -name '*.java' | sort)
"$JAVAC" -d "$out" "${sources[@]}"
"$JAVA" -cp "$out" com.fastshoppr.spike.dates.DatePhraseParserTest
