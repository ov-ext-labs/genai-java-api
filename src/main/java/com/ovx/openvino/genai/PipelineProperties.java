package com.ovx.openvino.genai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PipelineProperties {
    private final Map<String, Object> properties;

    private PipelineProperties(Builder builder) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PipelineProperties empty() {
        return builder().build();
    }

    public static PipelineProperties fromMap(Map<String, Object> properties) {
        Builder builder = builder();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public Map<String, Object> toMap() {
        return properties;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public enum AttentionBackend {
        PAGED_ATTENTION("PA"),
        SDPA("SDPA");

        private final String nativeValue;

        AttentionBackend(String nativeValue) {
            this.nativeValue = nativeValue;
        }

        public String nativeValue() {
            return nativeValue;
        }
    }

    public enum PerformanceHint {
        LATENCY,
        THROUGHPUT,
        CUMULATIVE_THROUGHPUT,
    }

    public static final class Builder {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(PipelineProperties properties) {
            this.properties.putAll(properties.properties);
        }

        public Builder put(String name, Object value) {
            this.properties.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public Builder cacheDir(String cacheDir) {
            return put("CACHE_DIR", Objects.requireNonNull(cacheDir, "cacheDir"));
        }

        public Builder enableMmap(boolean enabled) {
            return put("ENABLE_MMAP", enabled);
        }

        public Builder attentionBackend(AttentionBackend backend) {
            return put("ATTENTION_BACKEND", Objects.requireNonNull(backend, "backend").nativeValue());
        }

        public Builder attentionBackend(String backend) {
            return put("ATTENTION_BACKEND", Objects.requireNonNull(backend, "backend"));
        }

        public Builder performanceHint(PerformanceHint hint) {
            return put("PERFORMANCE_HINT", Objects.requireNonNull(hint, "hint").name());
        }

        public Builder performanceHint(String hint) {
            return put("PERFORMANCE_HINT", Objects.requireNonNull(hint, "hint"));
        }

        public Builder inferenceNumThreads(long threads) {
            return put("INFERENCE_NUM_THREADS", threads);
        }

        public Builder numStreams(long streams) {
            return put("NUM_STREAMS", streams);
        }

        public Builder numStreams(String streams) {
            return put("NUM_STREAMS", Objects.requireNonNull(streams, "streams"));
        }

        public Builder maxPromptLength(long tokens) {
            return put("MAX_PROMPT_LEN", tokens);
        }

        public Builder minResponseLength(long tokens) {
            return put("MIN_RESPONSE_LEN", tokens);
        }

        public Builder kvCachePrecision(String precision) {
            return put("KV_CACHE_PRECISION", Objects.requireNonNull(precision, "precision"));
        }

        public Builder dynamicQuantizationGroupSize(long groupSize) {
            return put("DYNAMIC_QUANTIZATION_GROUP_SIZE", groupSize);
        }

        public PipelineProperties build() {
            return new PipelineProperties(this);
        }
    }
}
