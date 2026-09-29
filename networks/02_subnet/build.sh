#!/bin/sh
set -eu
kotlinc Subnet.kt -include-runtime -d subnet.jar
