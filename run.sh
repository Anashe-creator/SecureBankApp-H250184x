#!/usr/bin/env bash
# Runs the application (data files are read from ./data)
cd "$(dirname "$0")"
[ -d out ] || ./build.sh
java -cp out com.securebank.Main
