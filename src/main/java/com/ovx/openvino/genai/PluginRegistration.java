package com.ovx.openvino.genai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PluginRegistration {
    private final String deviceName;
    private final String libraryPath;
    private final Map<String, Object> properties;

    private PluginRegistration(Builder builder) {
        this.deviceName = builder.deviceName;
        this.libraryPath = builder.libraryPath;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public static Builder builder(String deviceName, String libraryPath) {
        return new Builder(deviceName, libraryPath);
    }

    public static PluginRegistration gfx(String libraryPath) {
        return builder("GFX", libraryPath).build();
    }

    public String deviceName() {
        return deviceName;
    }

    public String libraryPath() {
        return libraryPath;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public static final class Builder {
        private final String deviceName;
        private final String libraryPath;
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private Builder(String deviceName, String libraryPath) {
            this.deviceName = Objects.requireNonNull(deviceName, "deviceName");
            this.libraryPath = Objects.requireNonNull(libraryPath, "libraryPath");
        }

        public Builder property(String name, Object value) {
            properties.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public PluginRegistration build() {
            return new PluginRegistration(this);
        }
    }
}
