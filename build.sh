#!/usr/bin/env bash
# Compiles all sources into ./out
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -Xlint:all -d out $(find src -name "*.java")
echo "Build successful."
