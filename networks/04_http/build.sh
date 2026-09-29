#!/bin/sh
set -eu
kotlinc HttpParser.kt -include-runtime -d http.jar
