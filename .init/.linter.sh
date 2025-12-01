#!/bin/bash
cd /home/kavia/workspace/code-generation/number-guessing-game-for-android-tv-215904-215913/android_tv_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

