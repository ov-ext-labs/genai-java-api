package com.ovx.openvino.genai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuntimeConfiguration {
    private final List<NativeLibraryReference> nativeLibraries;
    private final List<PluginRegistration> pluginRegistrations;
    private final Map<String, Object> runtimeProperties;

    private RuntimeConfiguration(Builder builder) {
        this.nativeLibraries = List.copyOf(builder.nativeLibraries);
        this.pluginRegistrations = List.copyOf(builder.pluginRegistrations);
        this.runtimeProperties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.runtimeProperties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RuntimeConfiguration defaultConfiguration() {
        return builder()
                .loadLibrary("ov_genai_java_jni")
                .build();
    }

    public List<NativeLibraryReference> nativeLibraries() {
        return nativeLibraries;
    }

    public List<PluginRegistration> pluginRegistrations() {
        return pluginRegistrations;
    }

    public Map<String, Object> runtimeProperties() {
        return runtimeProperties;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final List<NativeLibraryReference> nativeLibraries = new ArrayList<>();
        private final List<PluginRegistration> pluginRegistrations = new ArrayList<>();
        private final Map<String, Object> runtimeProperties = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(RuntimeConfiguration configuration) {
            nativeLibraries.addAll(configuration.nativeLibraries);
            pluginRegistrations.addAll(configuration.pluginRegistrations);
            runtimeProperties.putAll(configuration.runtimeProperties);
        }

        public Builder loadLibrary(String libraryName) {
            nativeLibraries.add(NativeLibraryReference.byName(libraryName));
            return this;
        }

        public Builder loadAbsoluteLibrary(String absolutePath) {
            nativeLibraries.add(NativeLibraryReference.byAbsolutePath(absolutePath));
            return this;
        }

        public Builder registerPlugin(PluginRegistration pluginRegistration) {
            pluginRegistrations.add(Objects.requireNonNull(pluginRegistration, "pluginRegistration"));
            return this;
        }

        public Builder runtimeProperty(String name, Object value) {
            runtimeProperties.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public RuntimeConfiguration build() {
            return new RuntimeConfiguration(this);
        }
    }
}
