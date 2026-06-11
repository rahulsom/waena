#!/usr/bin/env bash

set -euo pipefail

cd ..
test -d nothing || git clone https://github.com/rahulsom/nothing
cd nothing
git checkout main
git fetch --all --prune
git pull

./gradlew --include-build ../waena clean candidate