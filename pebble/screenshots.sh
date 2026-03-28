#!/bin/bash
#
# Generate Pebble app store screenshots for all platforms and scenes.
#
# Usage:
#   ./screenshots.sh                              # all platforms, all scenes
#   ./screenshots.sh --platforms "basalt chalk"    # specific platforms
#   ./screenshots.sh --scenes "1 2"               # specific scenes
#
# Scenes:
#   1 - Task list (My Tasks, grouped by date)
#   2 - Filter/list selection
#   3 - Task detail view
#   4 - Settings
#   5 - Task list with colored filter (Shopping)
#
# Requires: Rebble pebble SDK with emulator support
#
set -e

cd "$(dirname "$0")"

PLATFORMS="aplite basalt chalk diorite emery flint gabbro"
SCENES="1 2 3 4 5"
OUTPUT_DIR="screenshots"
WAIT="${SCREENSHOT_WAIT:-10}"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --platforms) PLATFORMS="$2"; shift 2 ;;
        --scenes)    SCENES="$2"; shift 2 ;;
        --wait)      WAIT="$2"; shift 2 ;;
        *)           echo "Unknown option: $1"; exit 1 ;;
    esac
done

SCENE_NAMES=(
    [1]="task_list"
    [2]="filters"
    [3]="detail"
    [4]="settings"
    [5]="shopping"
)

BUILD_DIR=$(mktemp -d)
trap "rm -rf $BUILD_DIR" EXIT

mkdir -p "$OUTPUT_DIR"
for platform in $PLATFORMS; do
    mkdir -p "$OUTPUT_DIR/$platform"
done

# Clean emulator state to avoid stale locks
pebble kill 2>/dev/null || true
pebble wipe 2>/dev/null || true

# Build all scenes upfront
for scene in $SCENES; do
    scene_name=${SCENE_NAMES[$scene]}
    echo "=== Building scene $scene: $scene_name ==="
    pebble clean
    SCREENSHOT_MODE=1 SCREENSHOT_SCENE="$scene" pebble build
    mv build/pebble.pbw "$BUILD_DIR/scene_${scene}.pbw"
done

# For each platform: boot emulator, take all scene screenshots, kill
for platform in $PLATFORMS; do
    echo "=== $platform ==="
    pebble kill 2>/dev/null || true
    sleep 2

    for scene in $SCENES; do
        scene_name=${SCENE_NAMES[$scene]}

        timeout 30 pebble install --emulator "$platform" \
            "$BUILD_DIR/scene_${scene}.pbw" || {
            echo "    WARN: install failed, retrying..."
            pebble kill 2>/dev/null || true
            sleep 2
            timeout 30 pebble install --emulator "$platform" \
                "$BUILD_DIR/scene_${scene}.pbw"
        }

        sleep "$WAIT"

        echo "  -> ${scene}_${scene_name}"
        timeout 15 pebble screenshot --emulator "$platform" --no-open \
            "$OUTPUT_DIR/$platform/${scene}_${scene_name}.png"
    done

    pebble kill 2>/dev/null || true
done

echo ""
echo "Done! Screenshots saved to $OUTPUT_DIR/"
echo "Run 'pebble clean && pebble build' to rebuild without screenshot mode."
