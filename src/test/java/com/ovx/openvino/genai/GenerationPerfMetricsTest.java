package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationPerfMetricsTest {
    @Test
    void readsKnownOpenVinoGenAiMetricKeys() {
        GenerationPerfMetrics metrics = GenerationPerfMetrics.from(
                Map.of(
                        "num_input_tokens", 12L,
                        "num_generated_tokens", 34,
                        "ttft_mean_ms", 56.5,
                        "tpot_mean_ms", "7.25",
                        "throughput_mean_tps", 19.75f));

        assertEquals(12L, metrics.numInputTokens());
        assertEquals(34L, metrics.numGeneratedTokens());
        assertEquals(56.5, metrics.ttftMeanMs());
        assertEquals(7.25, metrics.tpotMeanMs());
        assertEquals(19.75, metrics.throughputMeanTokensPerSecond());
        assertTrue(metrics.compactString().contains("input=12"));
    }

    @Test
    void ignoresMissingOrMalformedMetricValues() {
        GenerationPerfMetrics metrics = GenerationPerfMetrics.from(Map.of("num_input_tokens", "bad"));

        assertEquals(null, metrics.numInputTokens());
        assertEquals(null, metrics.numGeneratedTokens());
    }
}
