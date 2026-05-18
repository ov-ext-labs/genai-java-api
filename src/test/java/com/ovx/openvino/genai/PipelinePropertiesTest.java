package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PipelinePropertiesTest {
    @Test
    void builderUsesOpenVinoNativePropertyNames() {
        PipelineProperties properties = PipelineProperties.builder()
                .cacheDir("/data/user/0/app/cache/openvino-genai")
                .attentionBackend(PipelineProperties.AttentionBackend.SDPA)
                .performanceHint(PipelineProperties.PerformanceHint.LATENCY)
                .enableMmap(true)
                .numStreams(1)
                .inferenceNumThreads(4)
                .maxPromptLength(256)
                .minResponseLength(64)
                .kvCachePrecision("f16")
                .dynamicQuantizationGroupSize(32)
                .build();

        assertEquals("/data/user/0/app/cache/openvino-genai", properties.toMap().get("CACHE_DIR"));
        assertEquals("SDPA", properties.toMap().get("ATTENTION_BACKEND"));
        assertEquals("LATENCY", properties.toMap().get("PERFORMANCE_HINT"));
        assertEquals(true, properties.toMap().get("ENABLE_MMAP"));
        assertEquals(1L, properties.toMap().get("NUM_STREAMS"));
        assertEquals(4L, properties.toMap().get("INFERENCE_NUM_THREADS"));
        assertEquals(256L, properties.toMap().get("MAX_PROMPT_LEN"));
        assertEquals(64L, properties.toMap().get("MIN_RESPONSE_LEN"));
        assertEquals("f16", properties.toMap().get("KV_CACHE_PRECISION"));
        assertEquals(32L, properties.toMap().get("DYNAMIC_QUANTIZATION_GROUP_SIZE"));
    }

    @Test
    void mapRoundTripKeepsUnknownProperties() {
        PipelineProperties original = PipelineProperties.builder()
                .put("CUSTOM_PROPERTY", "value")
                .numStreams("AUTO")
                .build();

        PipelineProperties roundTripped = PipelineProperties.fromMap(original.toMap());
        assertEquals(original.toMap(), roundTripped.toMap());
        assertFalse(roundTripped.toMap().isEmpty());
    }
}
