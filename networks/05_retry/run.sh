#!/bin/sh
set -eu
exec java -jar retry.jar "$@"
