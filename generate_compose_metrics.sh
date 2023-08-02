#!/bin/sh

./gradlew clean :app:compileGoogleplayDebugKotlin \
    --no-build-cache \
    -Pandroidx.enableComposeCompilerMetrics=true \
    -Pandroidx.enableComposeCompilerReports=true \
    -PcomposeMetrics=$(pwd)
