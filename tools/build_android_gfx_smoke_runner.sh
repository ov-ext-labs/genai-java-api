#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${GENAI_JAVA_ANDROID_BUILD_DIR:-${REPO_DIR}/build/android/smoke-runner}"

: "${ANDROID_NDK:?Set ANDROID_NDK to the Android NDK root}"
: "${OPENVINO_ANDROID_DIR:?Set OPENVINO_ANDROID_DIR to the OpenVINO Android CMake package root}"
: "${OPENVINO_GENAI_ANDROID_DIR:?Set OPENVINO_GENAI_ANDROID_DIR to the OpenVINO GenAI Android CMake package root}"

ANDROID_ABI="${ANDROID_ABI:-arm64-v8a}"
ANDROID_PLATFORM="${ANDROID_PLATFORM:-35}"

cmake -S "${SCRIPT_DIR}" -B "${BUILD_DIR}" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="${ANDROID_ABI}" \
  -DANDROID_PLATFORM="${ANDROID_PLATFORM}" \
  -DANDROID_STL=c++_shared \
  -DOpenVINO_DIR="${OPENVINO_ANDROID_DIR}" \
  -DOpenVINOGenAI_DIR="${OPENVINO_GENAI_ANDROID_DIR}" >&2

cmake --build "${BUILD_DIR}" --target android_gfx_smoke_runner -j8 >&2

echo "${BUILD_DIR}/android_gfx_smoke_runner"
