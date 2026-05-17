package com.ovx.openvino.genai.internal;

import com.ovx.openvino.genai.NativeLibraryReference;
import com.ovx.openvino.genai.RuntimeConfiguration;

import java.util.Objects;

public final class NativeLoader {
    private static final Object LOCK = new Object();

    private static volatile boolean loaded;
    private static volatile RuntimeConfiguration configuration = RuntimeConfiguration.defaultConfiguration();

    private NativeLoader() {
    }

    public static void initialize(RuntimeConfiguration runtimeConfiguration) {
        Objects.requireNonNull(runtimeConfiguration, "runtimeConfiguration");
        synchronized (LOCK) {
            configuration = runtimeConfiguration;
            if (!loaded) {
                loadConfiguredLibraries(runtimeConfiguration);
                loaded = true;
            }
        }
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (!loaded) {
                loadConfiguredLibraries(configuration);
                loaded = true;
            }
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void loadConfiguredLibraries(RuntimeConfiguration runtimeConfiguration) {
        for (NativeLibraryReference reference : runtimeConfiguration.nativeLibraries()) {
            if (reference.isAbsolutePath()) {
                System.load(reference.absolutePath());
            } else {
                System.loadLibrary(reference.libraryName());
            }
        }
    }
}
