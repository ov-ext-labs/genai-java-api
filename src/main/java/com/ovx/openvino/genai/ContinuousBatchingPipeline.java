package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.NativeBindings;
import com.ovx.openvino.genai.internal.NativeResource;

import java.util.Map;
import java.util.Objects;

public final class ContinuousBatchingPipeline extends NativeResource {
    public ContinuousBatchingPipeline(
            String modelPath,
            SchedulerConfig schedulerConfig,
            DeviceSelection device,
            Map<String, Object> properties) {
        super(
                NativeBindings.cbCreate(
                        Objects.requireNonNull(modelPath, "modelPath"),
                        Objects.requireNonNull(device, "device").deviceName(),
                        Objects.requireNonNull(schedulerConfig, "schedulerConfig").toMap(),
                        properties == null ? Map.of() : Map.copyOf(properties)),
                NativeBindings::cbDispose);
    }

    public GenerationHandle addRequest(long requestId, String prompt, GenerationConfig generationConfig) {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(generationConfig, "generationConfig");
        return new GenerationHandle(
                NativeBindings.cbAddRequest(nativeHandle(), requestId, prompt, generationConfig.toMap()));
    }

    public void step() {
        NativeBindings.cbStep(nativeHandle());
    }

    public boolean hasNonFinishedRequests() {
        return NativeBindings.cbHasNonFinishedRequests(nativeHandle());
    }

    public PipelineMetrics getMetrics() {
        return NativeBindings.cbGetMetrics(nativeHandle());
    }
}
