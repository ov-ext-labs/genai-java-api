package com.ovx.openvino.genai;

import java.util.Objects;

public final class NativeLibraryReference {
    private final String libraryName;
    private final String absolutePath;

    private NativeLibraryReference(String libraryName, String absolutePath) {
        this.libraryName = libraryName;
        this.absolutePath = absolutePath;
    }

    public static NativeLibraryReference byName(String libraryName) {
        return new NativeLibraryReference(Objects.requireNonNull(libraryName, "libraryName"), null);
    }

    public static NativeLibraryReference byAbsolutePath(String absolutePath) {
        return new NativeLibraryReference(null, Objects.requireNonNull(absolutePath, "absolutePath"));
    }

    public boolean isAbsolutePath() {
        return absolutePath != null;
    }

    public String libraryName() {
        return libraryName;
    }

    public String absolutePath() {
        return absolutePath;
    }
}
