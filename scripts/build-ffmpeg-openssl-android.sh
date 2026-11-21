#!/usr/bin/env bash
set -euo pipefail

ABI="${1:?Usage: build-ffmpeg-openssl-android.sh <abi> <ffmpeg-source-dir> <output-dir>}"
FFMPEG_SRC="${2:?Missing FFmpeg source directory}"
OUTPUT_DIR="${3:?Missing output directory}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPENSSL_DIR="$ROOT/app/src/main/third_party/openssl/$ABI"
: "${ANDROID_NDK_HOME:=${ANDROID_NDK_ROOT:-}}"
if [[ -z "${ANDROID_NDK_HOME}" || ! -d "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64" ]]; then
  echo "ANDROID_NDK_HOME must point to an installed Android NDK (Linux host)." >&2
  exit 2
fi

TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
API=26
SYSROOT="$TOOLCHAIN/sysroot"

case "$ABI" in
  arm64-v8a)
    ARCH=aarch64
    CPU=armv8-a
    TRIPLE=aarch64-linux-android
    ;;
  armeabi-v7a)
    ARCH=arm
    CPU=armv7-a
    TRIPLE=armv7a-linux-androideabi
    ;;
  x86_64)
    ARCH=x86_64
    CPU=x86-64
    TRIPLE=x86_64-linux-android
    ;;
  *)
    echo "Unsupported ABI: $ABI" >&2
    exit 2
    ;;
esac

for f in "$OPENSSL_DIR/include/openssl/ssl.h" "$OPENSSL_DIR/lib/libssl.a" "$OPENSSL_DIR/lib/libcrypto.a"; do
  [[ -f "$f" ]] || { echo "Missing OpenSSL input: $f" >&2; exit 3; }
done

CC="$TOOLCHAIN/bin/${TRIPLE}${API}-clang"
CXX="$TOOLCHAIN/bin/${TRIPLE}${API}-clang++"
AR="$TOOLCHAIN/bin/llvm-ar"
RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
STRIP="$TOOLCHAIN/bin/llvm-strip"
NM="$TOOLCHAIN/bin/llvm-nm"
for f in "$CC" "$CXX" "$AR" "$RANLIB" "$STRIP" "$NM"; do
  [[ -x "$f" ]] || { echo "Missing NDK tool: $f" >&2; exit 4; }
done

# FFmpeg's configure detects OpenSSL through pkg-config. This local .pc file
# points it at the user-supplied Android static archives, not host OpenSSL.
PC_DIR="$OPENSSL_DIR/lib/pkgconfig"
mkdir -p "$PC_DIR"
cat > "$PC_DIR/openssl.pc" <<EOF
prefix=$OPENSSL_DIR
exec_prefix=\${prefix}
libdir=\${prefix}/lib
includedir=\${prefix}/include

Name: OpenSSL
Description: OpenSSL 3.3.2 Android static libraries
Version: 3.3.2
Libs: -L\${libdir} -lssl -lcrypto -ldl -llog
Cflags: -I\${includedir}
EOF

export PKG_CONFIG_LIBDIR="$PC_DIR"
export PKG_CONFIG_PATH="$PC_DIR"
export PKG_CONFIG_SYSROOT_DIR=""
export CC CXX AR RANLIB STRIP NM

BUILD_DIR="${FFMPEG_SRC}/build-${ABI}"
INSTALL_DIR="${FFMPEG_SRC}/install-${ABI}"
rm -rf "$BUILD_DIR" "$INSTALL_DIR"
mkdir -p "$BUILD_DIR" "$INSTALL_DIR" "$OUTPUT_DIR"

cd "$BUILD_DIR"
"$FFMPEG_SRC/configure" \
  --prefix="$INSTALL_DIR" \
  --target-os=android \
  --arch="$ARCH" \
  --cpu="$CPU" \
  --enable-cross-compile \
  --cc="$CC" \
  --cxx="$CXX" \
  --ar="$AR" \
  --ranlib="$RANLIB" \
  --strip="$STRIP" \
  --nm="$NM" \
  --sysroot="$SYSROOT" \
  --enable-pic \
  --enable-shared \
  --disable-static \
  --disable-programs \
  --disable-doc \
  --disable-debug \
  --disable-avdevice \
  --disable-postproc \
  --enable-openssl \
  --enable-protocol=https,tls,tcp,http,crypto,file,pipe \
  --extra-cflags="-fPIC -I$OPENSSL_DIR/include" \
  --extra-ldflags="-L$OPENSSL_DIR/lib -Wl,--exclude-libs,ALL" \
  --extra-libs="-lssl -lcrypto -ldl -llog"

if ! grep -q 'CONFIG_OPENSSL 1' config.h; then
  echo "FFmpeg configure did not enable OpenSSL; refusing to continue." >&2
  exit 5
fi
if ! grep -q 'CONFIG_HTTPS_PROTOCOL 1' config.h; then
  echo "FFmpeg configure did not enable HTTPS protocol; refusing to continue." >&2
  exit 6
fi

make -j2
make install

for lib in libavutil libswresample libavcodec libavformat libavfilter; do
  source="$INSTALL_DIR/lib/${lib}.so"
  [[ -e "$source" ]] || { echo "FFmpeg output missing: $source" >&2; exit 7; }
  cp -L "$source" "$OUTPUT_DIR/${lib}.so"
done

# Verify the new avformat shared object carries the TLS/OpenSSL implementation.
if ! "$NM" -D "$OUTPUT_DIR/libavformat.so" 2>/dev/null | grep -qiE 'TLS|SSL|OPENSSL'; then
  echo "Warning: no obvious TLS/OpenSSL symbols found in libavformat.so; inspect config.log before shipping."
fi
echo "Built FFmpeg 7.1 with OpenSSL/HTTPS for $ABI into $OUTPUT_DIR"
