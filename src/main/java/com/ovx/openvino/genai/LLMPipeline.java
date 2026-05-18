package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.NativeBindings;
import com.ovx.openvino.genai.internal.NativeResource;

import java.util.Map;
import java.util.Objects;

public final class LLMPipeline extends NativeResource {
    private final Object generationLock = new Object();

    public LLMPipeline(String modelPath) {
        this(modelPath, DeviceSelection.gfx(), PipelineProperties.empty());
    }

    public LLMPipeline(String modelPath, DeviceSelection device, PipelineProperties properties) {
        this(modelPath, device, properties == null ? Map.of() : properties.toMap());
    }

    public LLMPipeline(String modelPath, DeviceSelection device, Map<String, Object> properties) {
        super(
                NativeBindings.llmCreate(
                        Objects.requireNonNull(modelPath, "modelPath"),
                        Objects.requireNonNull(device, "device").deviceName(),
                        properties == null ? Map.of() : Map.copyOf(properties)),
                NativeBindings::llmDispose);
    }

    public GenerationResult generate(String prompt) {
        return generate(prompt, GenerationConfig.empty(), null);
    }

    public GenerationResult generate(String prompt, GenerationConfig config) {
        return generate(prompt, config, null);
    }

    public GenerationResult generate(String prompt, GenerationConfig config, StreamingCallback callback) {
        Objects.requireNonNull(prompt, "prompt");
        synchronized (generationLock) {
            return NativeBindings.llmGenerate(
                    nativeHandle(),
                    prompt,
                    config == null ? Map.of() : config.toMap(),
                    callback);
        }
    }

    public GenerationResult generate(ChatHistory history, GenerationConfig config) {
        return generate(history, config, null);
    }

    public GenerationResult generate(ChatHistory history, GenerationConfig config, StreamingCallback callback) {
        Objects.requireNonNull(history, "history");
        synchronized (generationLock) {
            return NativeBindings.llmGenerateChat(
                    nativeHandle(),
                    history.toJson(),
                    config == null ? Map.of() : config.toMap(),
                    callback);
        }
    }

    public GenerationConfig getGenerationConfig() {
        return GenerationConfig.fromMap(NativeBindings.llmGetGenerationConfig(nativeHandle()));
    }

    public void setGenerationConfig(GenerationConfig config) {
        Objects.requireNonNull(config, "config");
        NativeBindings.llmSetGenerationConfig(nativeHandle(), config.toMap());
    }

    public Tokenizer getTokenizer() {
        return new Tokenizer(NativeBindings.llmGetTokenizer(nativeHandle()));
    }
}
