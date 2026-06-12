#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1
exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar server.jar
