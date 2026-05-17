package com.ovx.openvino.genai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SchedulerConfig {
    private final Map<String, Object> properties;

    private SchedulerConfig(Builder builder) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        return properties;
    }

    public static final class Builder {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public Builder put(String name, Object value) {
            properties.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public SchedulerConfig build() {
            return new SchedulerConfig(this);
        }
    }
}
