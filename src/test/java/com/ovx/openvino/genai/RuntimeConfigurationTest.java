package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuntimeConfigurationTest {
    @Test
    void capturesAndroidPluginRegistrationAndPreloadOrder() {
        RuntimeConfiguration configuration = RuntimeConfiguration.builder()
                .loadLibrary("c++_shared")
                .loadLibrary("openvino")
                .loadLibrary("openvino_genai")
                .loadLibrary("ov_genai_java_jni")
                .registerPlugin(PluginRegistration.gfx("/data/local/tmp/libopenvino_gfx_plugin.so"))
                .runtimeProperty("platform", "android")
                .build();

        assertEquals(4, configuration.nativeLibraries().size());
        assertEquals("GFX", configuration.pluginRegistrations().get(0).deviceName());
        assertEquals("android", configuration.runtimeProperties().get("platform"));
        assertFalse(configuration.pluginRegistrations().isEmpty());
    }
}
