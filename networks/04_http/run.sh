#!/bin/sh
set -eu
exec java -jar http.jar "$@"
