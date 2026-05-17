package com.ovx.openvino.genai;

public enum GenerationStatus {
    RUNNING(0),
    FINISHED(1),
    IGNORED(2),
    CANCEL(3),
    STOP(4),
    UNKNOWN(-1);

    private final int nativeValue;

    GenerationStatus(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static GenerationStatus fromNativeValue(int nativeValue) {
        for (GenerationStatus status : values()) {
            if (status.nativeValue == nativeValue) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
