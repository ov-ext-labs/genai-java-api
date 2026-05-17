package com.ovx.openvino.genai;

public enum StreamingStatus {
    RUNNING(0),
    STOP(1),
    CANCEL(2);

    private final int nativeValue;

    StreamingStatus(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int nativeValue() {
        return nativeValue;
    }
}
