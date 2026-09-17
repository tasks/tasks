#!/bin/sh
set -eu

version=4.0.5
sha256=cc09a3ac41d60e6144e644bd3fcf97d47106d659c4a0b8965102581401e67c9c
out=${1:?usage: build-libical.sh <output directory>}

cmake=${CMAKE:-$(command -v cmake || true)}
for candidate in /opt/homebrew/bin/cmake /usr/local/bin/cmake /Applications/CMake.app/Contents/bin/cmake \
        "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"/cmake/*/bin/cmake; do
    [ -z "$cmake" ] && [ -x "$candidate" ] && cmake=$candidate
done
if [ -z "$cmake" ]; then
    echo "cmake is needed to build libical for iOS: brew install cmake, or set CMAKE=/path/to/cmake" >&2
    exit 1
fi

mkdir -p "$out"
cd "$out"

tarball=libical-$version.tar.gz
if ! echo "$sha256  $tarball" | shasum -a 256 -c --status; then
    curl -fsSL -o "$tarball" "https://github.com/libical/libical/releases/download/v$version/$tarball"
    echo "$sha256  $tarball" | shasum -a 256 -c
fi

rm -rf src build-* install-* libical.xcframework
mkdir src
tar -xzf "$tarball" -C src --strip-components 1

build() {
    "$cmake" -S src -B "build-$1" -G "Unix Makefiles" \
        -DCMAKE_SYSTEM_NAME=iOS \
        -DCMAKE_OSX_SYSROOT="$2" \
        -DCMAKE_OSX_ARCHITECTURES=arm64 \
        -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
        -DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="$PWD/install-$1" \
        -DLIBICAL_STATIC=ON \
        -DLIBICAL_CXX_BINDINGS=OFF \
        -DLIBICAL_JAVA_BINDINGS=OFF \
        -DLIBICAL_GLIB=OFF \
        -DLIBICAL_GOBJECT_INTROSPECTION=OFF \
        -DLIBICAL_GLIB_VAPI=OFF \
        -DLIBICAL_BUILD_DOCS=OFF \
        -DLIBICAL_BUILD_EXAMPLES=OFF \
        -DLIBICAL_BUILD_TESTING=OFF \
        -DCMAKE_DISABLE_FIND_PACKAGE_ICU=ON \
        -DCMAKE_DISABLE_FIND_PACKAGE_BerkeleyDB=ON \
        -DCMAKE_DISABLE_FIND_PACKAGE_PkgConfig=ON
    "$cmake" --build "build-$1" --target install --parallel "$(sysctl -n hw.ncpu)"
}

build ios-arm64 iphoneos
build ios-arm64-simulator iphonesimulator

xcodebuild -create-xcframework \
    -library install-ios-arm64/lib/libical.a -headers install-ios-arm64/include \
    -library install-ios-arm64-simulator/lib/libical.a -headers install-ios-arm64-simulator/include \
    -output libical.xcframework
