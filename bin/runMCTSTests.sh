#!/bin/bash
TARGET_DIR=./target/
PACKAGE_LIST=$(find packages/*)
PACKAGES=${PACKAGE_LIST//[[:space:]]/:}
java -cp $TARGET_DIR:$PACKAGES test.TestMCTS
