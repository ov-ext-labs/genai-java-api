package com.ovx.openvino.genai;

import java.util.Map;
import java.util.Objects;

public final class GenerationPerfMetrics {
    private static final String NUM_INPUT_TOKENS = "num_input_tokens";
    private static final String NUM_GENERATED_TOKENS = "num_generated_tokens";
    private static final String TTFT_MEAN_MS = "ttft_mean_ms";
    private static final String TPOT_MEAN_MS = "tpot_mean_ms";
    private static final String THROUGHPUT_MEAN_TPS = "throughput_mean_tps";

    private final Map<String, Object> metrics;

    private GenerationPerfMetrics(Map<String, Object> metrics) {
        this.metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    }

    public static GenerationPerfMetrics from(GenerationResult result) {
        Objects.requireNonNull(result, "result");
        return from(result.perfMetrics());
    }

    public static GenerationPerfMetrics from(Map<String, Object> metrics) {
        return new GenerationPerfMetrics(metrics);
    }

    public Long numInputTokens() {
        return longMetric(NUM_INPUT_TOKENS);
    }

    public Long numGeneratedTokens() {
        return longMetric(NUM_GENERATED_TOKENS);
    }

    public Double ttftMeanMs() {
        return doubleMetric(TTFT_MEAN_MS);
    }

    public Double tpotMeanMs() {
        return doubleMetric(TPOT_MEAN_MS);
    }

    public Double throughputMeanTokensPerSecond() {
        return doubleMetric(THROUGHPUT_MEAN_TPS);
    }

    public Map<String, Object> rawMetrics() {
        return metrics;
    }

    public String compactString() {
        return "{"
                + "input=" + numInputTokens()
                + ", generated=" + numGeneratedTokens()
                + ", ttftMs=" + ttftMeanMs()
                + ", tpotMs=" + tpotMeanMs()
                + ", throughput=" + throughputMeanTokensPerSecond()
                + "}";
    }

    private Long longMetric(String name) {
        Object value = metrics.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double doubleMetric(String name) {
        Object value = metrics.get(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
