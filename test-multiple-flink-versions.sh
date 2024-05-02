#!/bin/bash

set -o nounset
set -o errexit
set -o pipefail

echo >&2 "Testing with Flink 1.16"
./mvnw --batch-mode --define flink.version='[1.16,1.16.999]' clean verify

echo >&2 "Testing with Flink 1.17"
./mvnw --batch-mode --define flink.version='[1.17,1.17.999]' clean verify

echo >&2 "Testing with Flink 1.18"
./mvnw --batch-mode --define flink.version='[1.18,1.18.999]' clean verify

echo >&2 "Testing with Flink 1.19"
./mvnw --batch-mode --define flink.version='[1.19,1.19.999]' clean verify

echo >&2 "Testing with Flink 1.20-SNAPSHOT"
./mvnw --batch-mode --update-snapshots --define flink.version=1.20-SNAPSHOT clean verify
