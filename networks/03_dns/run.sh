#!/bin/sh
set -eu
exec java -jar dns.jar "$@"
