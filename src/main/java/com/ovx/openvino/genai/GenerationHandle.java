package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.NativeBindings;
import com.ovx.openvino.genai.internal.NativeResource;

public final class GenerationHandle extends NativeResource {
    GenerationHandle(long nativeHandle) {
        super(nativeHandle, NativeBindings::handleDispose);
    }

    public GenerationResult read() {
        return NativeBindings.handleRead(nativeHandle());
    }

    public GenerationResult readAll() {
        return NativeBindings.handleReadAll(nativeHandle());
    }

    public GenerationStatus status() {
        return GenerationStatus.fromNativeValue(NativeBindings.handleGetStatus(nativeHandle()));
    }

    public void stop() {
        NativeBindings.handleStop(nativeHandle());
    }

    public void cancel() {
        NativeBindings.handleCancel(nativeHandle());
    }
}
