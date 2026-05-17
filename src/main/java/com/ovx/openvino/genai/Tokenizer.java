package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.NativeBindings;
import com.ovx.openvino.genai.internal.NativeResource;

import java.util.Objects;

public final class Tokenizer extends NativeResource {
    Tokenizer(long nativeHandle) {
        super(nativeHandle, NativeBindings::tokenizerDispose);
    }

    public String applyChatTemplate(ChatHistory history, boolean addGenerationPrompt) {
        return applyChatTemplate(history, addGenerationPrompt, null, history.toolsJson(), history.extraContextJson());
    }

    public String applyChatTemplate(
            ChatHistory history,
            boolean addGenerationPrompt,
            String chatTemplate,
            String toolsJson,
            String extraContextJson) {
        Objects.requireNonNull(history, "history");
        return NativeBindings.tokenizerApplyChatTemplate(
                nativeHandle(),
                history.toJson(),
                addGenerationPrompt,
                chatTemplate,
                toolsJson,
                extraContextJson);
    }
}
