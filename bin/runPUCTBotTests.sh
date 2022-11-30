#!/bin/bash
TARGET_DIR=./target/
PACKAGE_LIST=$(find packages/*)
PACKAGES=${PACKAGE_LIST//[[:space:]]/:}
echo "java -cp $TARGET_DIR:$PACKAGES test.TestPUCTBot"
java -cp $TARGET_DIR:$PACKAGES test.TestPUCTBot

