package com.ovx.openvino.genai;

public final class PipelineMetrics {
    private final long requests;
    private final long scheduledRequests;
    private final float cacheUsage;
    private final float maxCacheUsage;
    private final float avgCacheUsage;
    private final float inferenceDurationMicros;
    private final long kvCacheSizeBytes;

    public PipelineMetrics(
            long requests,
            long scheduledRequests,
            float cacheUsage,
            float maxCacheUsage,
            float avgCacheUsage,
            float inferenceDurationMicros,
            long kvCacheSizeBytes) {
        this.requests = requests;
        this.scheduledRequests = scheduledRequests;
        this.cacheUsage = cacheUsage;
        this.maxCacheUsage = maxCacheUsage;
        this.avgCacheUsage = avgCacheUsage;
        this.inferenceDurationMicros = inferenceDurationMicros;
        this.kvCacheSizeBytes = kvCacheSizeBytes;
    }

    public long requests() {
        return requests;
    }

    public long scheduledRequests() {
        return scheduledRequests;
    }

    public float cacheUsage() {
        return cacheUsage;
    }

    public float maxCacheUsage() {
        return maxCacheUsage;
    }

    public float avgCacheUsage() {
        return avgCacheUsage;
    }

    public float inferenceDurationMicros() {
        return inferenceDurationMicros;
    }

    public long kvCacheSizeBytes() {
        return kvCacheSizeBytes;
    }
}
