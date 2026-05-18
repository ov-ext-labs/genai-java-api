# Architecture

`genai-java-api` exposes a small Java API over OpenVINO GenAI through JNI. The public package is `com.ovx.openvino.genai`; native implementation details stay under `com.ovx.openvino.genai.internal`.

## Java Surface

- `OpenVinoGenAiRuntime` owns native library loading and plugin registration.
- `RuntimeConfiguration` defines the native preload order and runtime plugin list.
- `LLMPipeline` wraps prompt and chat generation.
- `ChatHistory`, `ChatMessage`, and `ChatRole` provide chat request construction.
- `GenerationConfig` is a native-friendly map wrapper for OpenVINO GenAI generation parameters.
- `PipelineProperties` is a native-friendly map wrapper for OpenVINO/OpenVINO GenAI pipeline properties such as `CACHE_DIR`, `ATTENTION_BACKEND`, `PERFORMANCE_HINT`, `ENABLE_MMAP`, and static prompt/response limits.
- `GenerationPerfMetrics` reads the known native perf metric keys from `GenerationResult.perfMetrics()` while preserving the raw map for advanced consumers.
- `Tokenizer` exposes chat-template application from an `LLMPipeline` tokenizer.
- `ContinuousBatchingPipeline` and `GenerationHandle` define the async API surface, but the native bridge still treats those calls as pending implementation work.
- `com.ovx.openvino.genai.android` contains Android integration helpers: runtime asset staging, native preload/plugin bootstrap, and shared asset-directory copy/read utilities.

The Android convenience path defaults `LLMPipeline(String modelPath)` to `DeviceSelection.gfx()`. Use `LLMPipeline(String, DeviceSelection, PipelineProperties)` when an application needs a different OpenVINO device or additional properties.

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

On Android, `AndroidOpenVinoGenAiRuntime` is the convenience bootstrap around this same contract. It copies runtime metadata from packaged assets into app storage, mirrors plugin libraries where OpenVINO expects them, loads the preferred native libraries by absolute path, registers the requested device plugin, and initializes `OpenVinoGenAiRuntime`. `AndroidAssetBundle` is a small shared helper for reading and copying packaged asset directories; model selection, download policy, and cache invalidation remain application responsibilities.

Pipeline properties are intentionally separate from runtime initialization. `RuntimeConfiguration` loads native libraries and registers plugins; `PipelineProperties` is passed into `LLMPipeline` and becomes the C++ `ov::AnyMap`. For example:

```java
PipelineProperties properties = PipelineProperties.builder()
        .cacheDir(context.getCacheDir().toPath().resolve("openvino-genai").toString())
        .attentionBackend(PipelineProperties.AttentionBackend.SDPA)
        .performanceHint(PipelineProperties.PerformanceHint.LATENCY)
        .enableMmap(true)
        .build();

LLMPipeline pipeline = new LLMPipeline(modelDir, DeviceSelection.gfx(), properties);
```

Use `DeviceSelection.gfx()` for Android GFX deployments. `AndroidPipelineProperties.cpuLatency(...)` is a separate preset for applications that explicitly choose CPU execution.

## Publication Boundary

Public documentation should describe the Java API, JNI modes, Android packaging requirements, and validation commands. Do not publish local model directories, generated bundles, machine-specific paths, private architecture notes, or device logs.
