#!/usr/bin/env bash

set -euo pipefail

SQLITE_BUNDLED_VERSION=2.7.1
ANDROIDX_COMMIT=e4bd62f853853bf3522ed15681c58ef28b09ed44
BINDINGS_SHA256=f9f4747111a6635dffd5991126247ca0f2f6c851de1eaf4add3866a0518ac2e0
SQLITE_URL=https://www.sqlite.org/2025/sqlite-amalgamation-3500100.zip
SQLITE_SHA256=41716b44ac8777188c4c3f1f370f01c9cb9e3b6428eb5c981d086c35de2d9d3f
JDK_TAG=jdk-21-ga
JNI_H_SHA256=99e64ebbe749e6df284f852f11b3c73f6ea97baf15120428f40f887fe0616e61
JNI_MD_WINDOWS_SHA256=dbf96659c4c840b15ef40237db0c65657eca7a70904225fc984deb38999df515
JNI_MD_UNIX_SHA256=88cb5c33e306900dd35a78d5a439087123b8e91b0986bb5acb42cc9bd2fcc42e

SQLITE_FLAGS=(
    -DHAVE_USLEEP=1
    -DSQLITE_DEFAULT_AUTOVACUUM=1
    -DSQLITE_DEFAULT_MEMSTATUS=0
    -DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1
    -DSQLITE_ENABLE_COLUMN_METADATA
    -DSQLITE_ENABLE_FTS3
    -DSQLITE_ENABLE_FTS3_PARENTHESIS
    -DSQLITE_ENABLE_FTS4
    -DSQLITE_ENABLE_FTS5
    -DSQLITE_ENABLE_JSON1
    -DSQLITE_ENABLE_MATH_FUNCTIONS
    -DSQLITE_ENABLE_NORMALIZE
    -DSQLITE_ENABLE_RTREE
    -DSQLITE_ENABLE_STAT4
    -DSQLITE_HAVE_ISNAN
    -DSQLITE_OMIT_BUILTIN_TEST
    -DSQLITE_OMIT_DEPRECATED
    -DSQLITE_OMIT_PROGRESS_CALLBACK
    -DSQLITE_OMIT_SHARED_CACHE
    -DSQLITE_SECURE_DELETE
    -DSQLITE_TEMP_STORE=3
    -DSQLITE_THREADSAFE=2
)

versions="$(dirname "$0")/../gradle/libs.versions.toml"
grep -q "androidx.sqlite:sqlite-bundled\", version = \"$SQLITE_BUNDLED_VERSION\"" "$versions" || {
    echo "androidx-sqlite in libs.versions.toml is not $SQLITE_BUNDLED_VERSION, update the pins in $0" >&2
    exit 1
}

out=$(mkdir -p "$1" && cd "$1" && pwd)/natives
zig=(${ZIG:-zig})
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cd "$work"

curl -fsSL -o sqlite.zip "$SQLITE_URL"
echo "$SQLITE_SHA256  sqlite.zip" | sha256sum -c
unzip -q -j sqlite.zip '*/sqlite3.c' '*/sqlite3.h'

curl -fsSL -o sqlite_bindings.cpp \
    "https://raw.githubusercontent.com/androidx/androidx/$ANDROIDX_COMMIT/sqlite/sqlite-bundled/src/jvmAndAndroidMain/jni/sqlite_bindings.cpp"
echo "$BINDINGS_SHA256  sqlite_bindings.cpp" | sha256sum -c

jdk=https://raw.githubusercontent.com/openjdk/jdk/$JDK_TAG/src/java.base
mkdir -p include/windows include/unix
curl -fsSL -o include/jni.h "$jdk/share/native/include/jni.h"
curl -fsSL -o include/windows/jni_md.h "$jdk/windows/native/include/jni_md.h"
curl -fsSL -o include/unix/jni_md.h "$jdk/unix/native/include/jni_md.h"
sha256sum -c <<EOF
$JNI_H_SHA256  include/jni.h
$JNI_MD_WINDOWS_SHA256  include/windows/jni_md.h
$JNI_MD_UNIX_SHA256  include/unix/jni_md.h
EOF

build() {
    local target=$1 md=$2 dir=$3 lib=$4 type=$5
    mkdir "$target"
    "${zig[@]}" cc -target "$target" -O3 "${SQLITE_FLAGS[@]}" -c sqlite3.c -o "$target/sqlite3.o"
    "${zig[@]}" c++ -target "$target" -O3 -fno-exceptions -fno-rtti -Wno-writable-strings \
        -Iinclude -Iinclude/"$md" -I. -c sqlite_bindings.cpp -o "$target/sqlite_bindings.o"
    "${zig[@]}" cc -target "$target" -shared -s -Wl,-headerpad,0x1000 -o "$target/$lib" "$target/sqlite_bindings.o" "$target/sqlite3.o"
    file "$target/$lib" | grep -q "$type" || { file "$target/$lib" >&2; echo "expected $type" >&2; exit 1; }
    mkdir -p "$out/$dir"
    cp "$target/$lib" "$out/$dir/"
    echo "$out/$dir/$lib"
}

build aarch64-windows-gnu windows windows_arm64 sqliteJni.dll "PE32+ executable (DLL).*Aarch64"
build x86_64-macos.10.15 unix osx_x64 libsqliteJni.dylib "Mach-O 64-bit.*x86_64"
