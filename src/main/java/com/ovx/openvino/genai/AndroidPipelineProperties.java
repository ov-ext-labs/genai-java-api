package com.ovx.openvino.genai;

import java.io.File;
import java.util.Objects;

public final class AndroidPipelineProperties {
    private AndroidPipelineProperties() {
    }

    public static PipelineProperties cpuLatency(File cacheDir) {
        return cpuLatency(cacheDir, CpuLatencyOptions.defaultOptions());
    }

    public static PipelineProperties cpuLatency(File cacheDir, CpuLatencyOptions options) {
        Objects.requireNonNull(cacheDir, "cacheDir");
        CpuLatencyOptions resolvedOptions = options == null ? CpuLatencyOptions.defaultOptions() : options;
        PipelineProperties.Builder builder = PipelineProperties.builder()
                .cacheDir(cacheDir.getAbsolutePath())
                .attentionBackend(PipelineProperties.AttentionBackend.SDPA)
                .performanceHint(PipelineProperties.PerformanceHint.LATENCY)
                .numStreams(resolvedOptions.numStreams())
                .inferenceNumThreads(resolvedOptions.inferenceNumThreads())
                .kvCachePrecision(resolvedOptions.kvCachePrecision())
                .dynamicQuantizationGroupSize(resolvedOptions.dynamicQuantizationGroupSize());
        if (resolvedOptions.enableMmap()) {
            builder.enableMmap(true);
        }
        return builder.build();
    }

    public static int defaultInferenceThreads() {
        return defaultInferenceThreads(Runtime.getRuntime().availableProcessors());
    }

    public static int defaultInferenceThreads(int availableProcessors) {
        if (availableProcessors >= 8) {
            return 4;
        }
        if (availableProcessors >= 4) {
            return 3;
        }
        return Math.max(1, availableProcessors);
    }

    public static final class CpuLatencyOptions {
        private final long inferenceNumThreads;
        private final long numStreams;
        private final String kvCachePrecision;
        private final long dynamicQuantizationGroupSize;
        private final boolean enableMmap;

        private CpuLatencyOptions(Builder builder) {
            this.inferenceNumThreads = builder.inferenceNumThreads;
            this.numStreams = builder.numStreams;
            this.kvCachePrecision = builder.kvCachePrecision;
            this.dynamicQuantizationGroupSize = builder.dynamicQuantizationGroupSize;
            this.enableMmap = builder.enableMmap;
        }

        public static CpuLatencyOptions defaultOptions() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        public long inferenceNumThreads() {
            return inferenceNumThreads;
        }

        public long numStreams() {
            return numStreams;
        }

        public String kvCachePrecision() {
            return kvCachePrecision;
        }

        public long dynamicQuantizationGroupSize() {
            return dynamicQuantizationGroupSize;
        }

        public boolean enableMmap() {
            return enableMmap;
        }

        public static final class Builder {
            private long inferenceNumThreads = AndroidPipelineProperties.defaultInferenceThreads();
            private long numStreams = 1;
            private String kvCachePrecision = "u8";
            private long dynamicQuantizationGroupSize = 32;
            private boolean enableMmap = true;

            private Builder() {
            }

            public Builder inferenceNumThreads(long inferenceNumThreads) {
                this.inferenceNumThreads = inferenceNumThreads;
                return this;
            }

            public Builder numStreams(long numStreams) {
                this.numStreams = numStreams;
                return this;
            }

            public Builder kvCachePrecision(String kvCachePrecision) {
                this.kvCachePrecision = Objects.requireNonNull(kvCachePrecision, "kvCachePrecision");
                return this;
            }

            public Builder dynamicQuantizationGroupSize(long dynamicQuantizationGroupSize) {
                this.dynamicQuantizationGroupSize = dynamicQuantizationGroupSize;
                return this;
            }

            public Builder enableMmap(boolean enableMmap) {
                this.enableMmap = enableMmap;
                return this;
            }

            public CpuLatencyOptions build() {
                return new CpuLatencyOptions(this);
            }
        }
    }
}
