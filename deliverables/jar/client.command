#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1
exec java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar client.jar
