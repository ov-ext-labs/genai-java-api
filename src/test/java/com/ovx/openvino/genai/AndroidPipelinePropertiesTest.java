package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AndroidPipelinePropertiesTest {
    @Test
    void cpuLatencyUsesAndroidDefaults() {
        PipelineProperties properties = AndroidPipelineProperties.cpuLatency(new File("/data/user/0/app/cache/ov"));

        assertEquals("/data/user/0/app/cache/ov", properties.toMap().get("CACHE_DIR"));
        assertEquals("SDPA", properties.toMap().get("ATTENTION_BACKEND"));
        assertEquals("LATENCY", properties.toMap().get("PERFORMANCE_HINT"));
        assertEquals(true, properties.toMap().get("ENABLE_MMAP"));
        assertEquals(1L, properties.toMap().get("NUM_STREAMS"));
        assertEquals("u8", properties.toMap().get("KV_CACHE_PRECISION"));
        assertEquals(32L, properties.toMap().get("DYNAMIC_QUANTIZATION_GROUP_SIZE"));
    }

    @Test
    void defaultInferenceThreadsIsConservativeForPhones() {
        assertEquals(4, AndroidPipelineProperties.defaultInferenceThreads(8));
        assertEquals(3, AndroidPipelineProperties.defaultInferenceThreads(4));
        assertEquals(1, AndroidPipelineProperties.defaultInferenceThreads(1));
    }

    @Test
    void cpuLatencyAcceptsExplicitOptions() {
        AndroidPipelineProperties.CpuLatencyOptions options =
                AndroidPipelineProperties.CpuLatencyOptions.builder()
                        .inferenceNumThreads(2)
                        .numStreams(3)
                        .kvCachePrecision("f16")
                        .dynamicQuantizationGroupSize(64)
                        .build();

        PipelineProperties properties = AndroidPipelineProperties.cpuLatency(
                new File("/data/user/0/example/cache/openvino-genai"), options);

        assertEquals(2L, properties.toMap().get("INFERENCE_NUM_THREADS"));
        assertEquals(3L, properties.toMap().get("NUM_STREAMS"));
        assertEquals("f16", properties.toMap().get("KV_CACHE_PRECISION"));
        assertEquals(64L, properties.toMap().get("DYNAMIC_QUANTIZATION_GROUP_SIZE"));
    }
}
