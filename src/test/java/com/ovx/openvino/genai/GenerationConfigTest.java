package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GenerationConfigTest {
    @Test
    void builderUsesNativeFriendlyPropertyNames() {
        GenerationConfig config = GenerationConfig.builder()
                .maxNewTokens(256)
                .doSample(true)
                .topP(0.9)
                .jsonSchema("{\"type\":\"object\"}")
                .build();

        assertEquals(256L, config.toMap().get("max_new_tokens"));
        assertEquals(true, config.toMap().get("do_sample"));
        assertEquals(0.9, config.toMap().get("top_p"));
        assertEquals("{\"type\":\"object\"}", config.toMap().get("json_schema"));
    }

    @Test
    void mapRoundTripKeepsValues() {
        GenerationConfig original = GenerationConfig.builder()
                .temperature(0.7)
                .numBeams(4)
                .build();

        GenerationConfig roundTripped = GenerationConfig.fromMap(original.toMap());
        assertEquals(original.toMap(), roundTripped.toMap());
        assertFalse(roundTripped.toMap().isEmpty());
    }
}
