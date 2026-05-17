package com.ovx.openvino.genai;

@FunctionalInterface
public interface StreamingCallback {
    StreamingStatus onText(String chunk);
}
