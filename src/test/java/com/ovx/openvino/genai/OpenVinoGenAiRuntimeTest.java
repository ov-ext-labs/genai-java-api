package com.ovx.openvino.genai;

public final class OpenVinoGenAiRuntimeTest {
    public static void main(String[] args) {
        if (!"bootstrap".equals(OpenVinoGenAiRuntime.version())) {
            throw new IllegalStateException("Unexpected version");
        }
    }
}
