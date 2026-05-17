# genai-java-api

Java/JNI SDK surface for OpenVINO GenAI with an Android-first runtime path and explicit OpenVINO plugin registration.

## Scope

Current implementation focus:
- Java API surface for `LLMPipeline`, chat history, generation configuration, tokenizer access, continuous batching, and generation handles
- Android runtime bootstrap with explicit native preload ordering
- explicit `GFX` plugin registration for Android deployments
- JNI bridge selection between a real OpenVINO GenAI bridge and a stub bridge for API-only builds

## Current State

The Java package is `com.ovx.openvino.genai`.

The native side now has two modes:
- `STUB`: fallback when OpenVINO GenAI is not discoverable by CMake
- `REAL`: direct JNI bridge for `LLMPipeline`, `GenerationConfig`, `Tokenizer`, chat generation, and streaming callbacks

Currently implemented in `REAL` mode:
- `runtimeVersion`
- `runtimeConfigure` with plugin registration support
- `LLMPipeline` create/dispose
- `generate(prompt)` and `generate(ChatHistory)` with streaming callback support
- `getGenerationConfig` / `setGenerationConfig`
- `getTokenizer`
- `Tokenizer.applyChatTemplate`

Still stubbed:
- `ContinuousBatchingPipeline`
- `GenerationHandle`

Build selection is controlled by `OV_GENAI_JNI_MODE`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full mode matrix.

## Runtime Bootstrap

The intended Android load order is explicit and application-controlled, for example:

1. `c++_shared`
2. `openvino`
3. `openvino_genai`
4. `ov_genai_java_jni`
5. runtime plugin registration for `GFX`

Example bootstrap:

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

## Build Examples

Desktop stub build:

```bash
export JAVA_HOME=/path/to/jdk-17
cmake -S . -B build
cmake --build build
```

Android real build against existing OpenVINO and OpenVINO GenAI Android package configurations:

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

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Android GFX packaging](docs/ANDROID_GFX.md)
- [Development and validation](docs/DEVELOPMENT.md)

## Roadmap

The next implementation step is to finish the remaining async layer against upstream `openvino.genai`:
- `ContinuousBatchingPipeline`
- `GenerationHandle`
- richer `AnyMap` coverage for advanced scheduler/plugin settings
- Android runtime validation on a fully working `GFX` generation path
