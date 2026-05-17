package com.ovx.openvino.genai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryTest {
    @Test
    void serializesMessagesToolsAndExtraContext() {
        ChatHistory history = ChatHistory.builder()
                .addSystem("You are a mobile assistant")
                .addUser("Call the camera tool")
                .toolsJson("[{\"name\":\"camera.open\"}]")
                .extraContextJson("{\"device\":\"android\"}")
                .build();

        String json = history.toJson();

        assertTrue(json.contains("\"messages\""));
        assertTrue(json.contains("\"role\":\"system\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"tools\":[{\"name\":\"camera.open\"}]"));
        assertTrue(json.contains("\"extra_context\":{\"device\":\"android\"}"));
    }
}
