package com.ovx.openvino.genai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GenerationResult {
    private final long requestId;
    private final List<String> texts;
    private final List<Float> scores;
    private final GenerationStatus status;
    private final Map<String, Object> perfMetrics;
    private final Map<String, Object> extendedPerfMetrics;
    private final List<String> parsedJson;

    public GenerationResult(
            long requestId,
            List<String> texts,
            List<Float> scores,
            GenerationStatus status,
            Map<String, Object> perfMetrics,
            Map<String, Object> extendedPerfMetrics,
            List<String> parsedJson) {
        this.requestId = requestId;
        this.texts = texts == null ? List.of() : List.copyOf(texts);
        this.scores = scores == null ? List.of() : List.copyOf(scores);
        this.status = status == null ? GenerationStatus.UNKNOWN : status;
        this.perfMetrics = perfMetrics == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(perfMetrics));
        this.extendedPerfMetrics = extendedPerfMetrics == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(extendedPerfMetrics));
        this.parsedJson = parsedJson == null ? List.of() : List.copyOf(parsedJson);
    }

    public long requestId() {
        return requestId;
    }

    public List<String> texts() {
        return texts;
    }

    public List<Float> scores() {
        return scores;
    }

    public GenerationStatus status() {
        return status;
    }

    public Map<String, Object> perfMetrics() {
        return perfMetrics;
    }

    public Map<String, Object> extendedPerfMetrics() {
        return extendedPerfMetrics;
    }

    public List<String> parsedJson() {
        return parsedJson;
    }

    public String firstText() {
        return texts.isEmpty() ? null : texts.get(0);
    }

    @Override
    public String toString() {
        if (texts.isEmpty()) {
            return "";
        }
        if (texts.size() == 1) {
            return texts.get(0);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            float score = i < scores.size() ? scores.get(i) : 0.0f;
            builder.append(score).append(": ").append(texts.get(i));
        }
        return builder.toString();
    }
}
