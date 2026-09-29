#!/bin/sh
set -eu
kotlinc DnsClient.kt -include-runtime -d dns.jar
