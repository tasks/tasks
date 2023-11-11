#!/bin/sh

./gradlew clean :app:compileGoogleplayReleaseKotlin \
    --no-build-cache \
    -Pandroidx.enableComposeCompilerMetrics=true \
    -Pandroidx.enableComposeCompilerReports=true \
    -PcomposeMetrics=$(pwd)
