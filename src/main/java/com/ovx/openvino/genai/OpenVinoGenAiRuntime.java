package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.NativeBindings;
import com.ovx.openvino.genai.internal.NativeLoader;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class OpenVinoGenAiRuntime {
    public static final String JAVA_API_VERSION = "0.1.0-SNAPSHOT";

    private static final AtomicReference<RuntimeConfiguration> CONFIGURATION =
            new AtomicReference<>(RuntimeConfiguration.defaultConfiguration());

    private OpenVinoGenAiRuntime() {
    }

    public static void initialize(RuntimeConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        NativeLoader.initialize(configuration);
        NativeBindings.runtimeConfigure(configuration.pluginRegistrations());
        CONFIGURATION.set(configuration);
    }

    public static RuntimeConfiguration configuration() {
        return CONFIGURATION.get();
    }

    public static boolean isInitialized() {
        return NativeLoader.isLoaded();
    }

    public static void ensureInitialized() {
        if (!isInitialized()) {
            initialize(CONFIGURATION.get());
        }
    }

    public static String version() {
        if (!isInitialized()) {
            return JAVA_API_VERSION + "+java";
        }
        return NativeBindings.runtimeVersion();
    }
}
