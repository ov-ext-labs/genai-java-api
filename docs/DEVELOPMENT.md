# Development

## Java Validation

Run the Java unit tests before publishing API changes:

```bash
gradle test
```

The tests cover the public value objects and Android bootstrap configuration. They do not require OpenVINO GenAI native binaries.

Android-specific integration helpers live under `src/android/java` and depend on Android framework classes. They are source-level helpers for Android app integrations, not part of the desktop `gradle test` compile path.

## Native Validation

Build the default native target in stub or auto mode:

```bash
export JAVA_HOME=/path/to/jdk-17
cmake -S . -B build
cmake --build build
```

`JAVA_HOME` must point to a JDK with JNI headers when CMake configures a desktop build.

Build the real Android JNI bridge only when OpenVINO and OpenVINO GenAI Android package configurations are available:

```bash
cmake -S . -B build/android-jni-real -G Ninja \
  -DOV_GENAI_JNI_MODE=REAL \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=35 \
  -DANDROID_STL=c++_shared \
  -DOpenVINO_DIR=/path/to/openvino-android \
  -DOpenVINOGenAI_DIR=/path/to/openvino-genai-android

cmake --build build/android-jni-real
```

## Publication Hygiene

Before committing public-facing changes:

```bash
git diff --check
rg -n "(?:anes)terov|/(?:Users)|[.](?:tmp/local_docs)|deep-(?:research)" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
rg -n "(?:sec)ret|(?:pass)word|api[_-]?key|access[_-]?token" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
```

Keep these artifacts local-only:

- `.tmp/`
- `.internal-docs/`
- `build/`
- `.gradle/`
- device logs, smoke bundles, converted model directories, and machine-specific paths

Do not edit or stage `AGENTS.md` or local technical notes as part of a publication cleanup unless the user explicitly asks for that file.

## Android Asset Staging Check

The asset staging helper can be checked without a device by creating a temporary package-shaped tree under `build/` and running:

```bash
tools/stage_android_runtime_assets.py \
  --package-dir build/package-smoke/openvino-android \
  --abi arm64-v8a \
  --package-name openvino-android-2026.1.0 \
  --output build/package-smoke/assets/openvino-runtime
```

Use real package paths for release validation. Keep generated assets under ignored build directories.
