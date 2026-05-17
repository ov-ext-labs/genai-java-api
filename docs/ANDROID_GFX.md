# Android GFX Packaging

The Android path is designed for explicit packaging. The application or smoke bundle must provide OpenVINO runtime libraries, OpenVINO GenAI libraries, `libopenvino_gfx_plugin.so`, `libc++_shared.so`, and an OpenVINO GenAI model directory.

## Required Inputs

Set these variables before using the helper scripts:

```bash
export ANDROID_NDK=/path/to/android-ndk
export OPENVINO_ANDROID_DIR=/path/to/openvino-android-cmake-package
export OPENVINO_GENAI_ANDROID_DIR=/path/to/openvino-genai-android-cmake-package

export OPENVINO_ANDROID_LIB_DIR=/path/to/openvino-android-libs
export OPENVINO_GENAI_ANDROID_BUILD=/path/to/openvino-genai-android-libs
export OPENVINO_ANDROID_TBB_DIR=/path/to/android-tbb-libs
export MODEL_DIR=/path/to/openvino-genai-model
```

Optional variables:

- `ANDROID_ABI`, default `arm64-v8a`
- `ANDROID_PLATFORM`, default `35`
- `GENAI_JAVA_ANDROID_BUILD_DIR`, default `build/android/smoke-runner`
- `GENAI_JAVA_ANDROID_BUNDLE_DIR`, default `build/android/device-bundle`
- `ADB_SERIAL`, used when more than one device is connected
- `DEVICE_DIR`, default `/data/local/tmp/ov_genai_android`
- `RUNNER_PATH`, used to reuse an existing `android_gfx_smoke_runner`
- `LIBCXX_SHARED`, used when the NDK layout requires an explicit `libc++_shared.so` path

## Build The Smoke Runner

```bash
tools/build_android_gfx_smoke_runner.sh
```

The script configures `tools/CMakeLists.txt` with the Android NDK toolchain and links the native smoke runner against `openvino::runtime` and `openvino::genai`.

## Deploy The Bundle

```bash
tools/deploy_android_gfx_bundle.sh
```

The deploy script assembles a local ignored bundle under `build/android/device-bundle`, verifies the required model files, generates `plugins.xml` for `GFX`, pushes the bundle to the device, and verifies the remote model files.

Run the smoke runner on the device:

```bash
adb shell 'cd /data/local/tmp/ov_genai_android && export LD_LIBRARY_PATH=$PWD && ./android_gfx_smoke_runner model "Hello from OpenVINO GenAI" 64'
```

The runner first calls `Core.compile_model(..., "GFX")`, then creates `ov::genai::LLMPipeline` on `GFX`, reads the generation config, and performs one generation request.

## App Integration

Package native libraries in the same relative location expected by the app, then initialize:

```java
RuntimeConfiguration configuration = RuntimeConfiguration.builder()
        .loadLibrary("c++_shared")
        .loadLibrary("openvino")
        .loadLibrary("openvino_genai")
        .loadLibrary("ov_genai_java_jni")
        .registerPlugin(PluginRegistration.gfx("/data/local/tmp/ov_genai_android/libopenvino_gfx_plugin.so"))
        .build();

OpenVinoGenAiRuntime.initialize(configuration);
```

Use `DeviceSelection.gfx()` for the active Android path unless a task explicitly requires another OpenVINO device.
