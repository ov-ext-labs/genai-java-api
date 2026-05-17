#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUNDLE_DIR="${GENAI_JAVA_ANDROID_BUNDLE_DIR:-${REPO_DIR}/build/android/device-bundle}"
DEVICE_DIR="${DEVICE_DIR:-/data/local/tmp/ov_genai_android}"

: "${MODEL_DIR:?Set MODEL_DIR to an OpenVINO GenAI model directory}"
: "${OPENVINO_ANDROID_LIB_DIR:?Set OPENVINO_ANDROID_LIB_DIR to the OpenVINO Android library directory}"
: "${OPENVINO_GENAI_ANDROID_BUILD:?Set OPENVINO_GENAI_ANDROID_BUILD to the OpenVINO GenAI Android library directory}"
: "${OPENVINO_ANDROID_TBB_DIR:?Set OPENVINO_ANDROID_TBB_DIR to the Android oneTBB library directory}"
: "${ANDROID_NDK:?Set ANDROID_NDK to the Android NDK root}"
: "${RUNNER_PATH:=$("${SCRIPT_DIR}/build_android_gfx_smoke_runner.sh" | tail -n 1)}"
: "${ADB_SERIAL:=}"

find_libcxx_shared() {
  find "${ANDROID_NDK}/toolchains/llvm/prebuilt" \
    \( -path "*/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" \
       -o -path "*/sysroot/usr/lib/aarch64-linux-android/*/libc++_shared.so" \) \
    -print -quit
}

LIBCXX_SHARED="${LIBCXX_SHARED:-$(find_libcxx_shared)}"

ADB=(adb)
if [[ -n "${ADB_SERIAL}" ]]; then
  ADB+=( -s "${ADB_SERIAL}" )
fi

rm -rf "${BUNDLE_DIR}"
mkdir -p "${BUNDLE_DIR}/model"

REQUIRED_MODEL_FILES=(
  openvino_model.xml
  openvino_model.bin
  openvino_tokenizer.xml
  openvino_tokenizer.bin
  openvino_detokenizer.xml
  openvino_detokenizer.bin
)

cp "${OPENVINO_ANDROID_LIB_DIR}/libopenvino.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_ANDROID_LIB_DIR}/libopenvino_gfx_plugin.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_ANDROID_LIB_DIR}/libopenvino_ir_frontend.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_ANDROID_LIB_DIR}/libopenvino_onnx_frontend.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_GENAI_ANDROID_BUILD}/libopenvino_genai.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_GENAI_ANDROID_BUILD}/libopenvino_tokenizers.so" "${BUNDLE_DIR}/"
cp "${OPENVINO_ANDROID_TBB_DIR}/libtbb.so" "${BUNDLE_DIR}/"
cp "${LIBCXX_SHARED}" "${BUNDLE_DIR}/"
cp "${RUNNER_PATH}" "${BUNDLE_DIR}/"
cp "${MODEL_DIR}"/* "${BUNDLE_DIR}/model/"

for required_file in "${REQUIRED_MODEL_FILES[@]}"; do
  if [[ ! -f "${BUNDLE_DIR}/model/${required_file}" ]]; then
    echo "Missing required model artifact in bundle: ${required_file}" >&2
    exit 1
  fi
done

cat > "${BUNDLE_DIR}/plugins.xml" <<'EOF'
<ie>
    <plugins>
        <plugin name="GFX" location="libopenvino_gfx_plugin.so">
        </plugin>
    </plugins>
</ie>
EOF

"${ADB[@]}" shell "rm -rf ${DEVICE_DIR} && mkdir -p ${DEVICE_DIR}"
"${ADB[@]}" push "${BUNDLE_DIR}/." "${DEVICE_DIR}/"
"${ADB[@]}" shell "cd ${DEVICE_DIR} && chmod +x android_gfx_smoke_runner && ls -lh"

for required_file in "${REQUIRED_MODEL_FILES[@]}"; do
  if ! "${ADB[@]}" shell "[ -f '${DEVICE_DIR}/model/${required_file}' ]"; then
    echo "Remote bundle is missing required model artifact: ${required_file}" >&2
    echo "adb push likely finished partially; rerun the deploy script." >&2
    exit 1
  fi
done

echo "Bundle deployed to ${DEVICE_DIR}"
echo "Run with:"
echo "  adb shell 'cd ${DEVICE_DIR} && export LD_LIBRARY_PATH=${DEVICE_DIR} && ./android_gfx_smoke_runner model \"Hello from OpenVINO GenAI\" 64'"
