#!/bin/sh
set -eu
kotlinc PacketParser.kt -include-runtime -d packet.jar
