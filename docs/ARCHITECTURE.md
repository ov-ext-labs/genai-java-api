# Architecture

`genai-java-api` exposes a small Java API over OpenVINO GenAI through JNI. The public package is `com.ovx.openvino.genai`; native implementation details stay under `com.ovx.openvino.genai.internal`.

## Java Surface

- `OpenVinoGenAiRuntime` owns native library loading and plugin registration.
- `RuntimeConfiguration` defines the native preload order and runtime plugin list.
- `LLMPipeline` wraps prompt and chat generation.
- `ChatHistory`, `ChatMessage`, and `ChatRole` provide chat request construction.
- `GenerationConfig` is a native-friendly map wrapper for OpenVINO GenAI generation parameters.
- `Tokenizer` exposes chat-template application from an `LLMPipeline` tokenizer.
- `ContinuousBatchingPipeline` and `GenerationHandle` define the async API surface, but the native bridge still treats those calls as pending implementation work.

The Android convenience path defaults `LLMPipeline(String modelPath)` to `DeviceSelection.gfx()`. Use `LLMPipeline(String, DeviceSelection, Map<String, Object>)` when an application needs a different OpenVINO device or additional properties.

## Native Bridge Modes

`OV_GENAI_JNI_MODE` selects the native bridge at CMake configure time:

- `AUTO`: build the real bridge when `OpenVINOGenAI` is found, otherwise build the stub bridge.
- `REAL`: require the `OpenVINOGenAI` CMake package and link `openvino::genai`.
- `STUB`: build an API-only JNI library that reports its stub version and throws `UnsupportedOperationException` for model execution.

The stub bridge is useful for Java API compilation, tests, IDE indexing, and CI environments that do not provide OpenVINO GenAI native artifacts. It is not a runtime inference implementation.

## Runtime Initialization

Applications should initialize the runtime before creating pipelines:

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

Library ordering is intentionally application-controlled because Android deployments often package OpenVINO, OpenVINO GenAI, plugin libraries, and `c++_shared` separately.

## Publication Boundary

Public documentation should describe the Java API, JNI modes, Android packaging requirements, and validation commands. Do not publish local model directories, generated bundles, machine-specific paths, private architecture notes, or device logs.
