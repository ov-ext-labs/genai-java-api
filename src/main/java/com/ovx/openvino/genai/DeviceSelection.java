package com.ovx.openvino.genai;

import java.util.Objects;

public final class DeviceSelection {
    private final String deviceName;

    private DeviceSelection(String deviceName) {
        this.deviceName = Objects.requireNonNull(deviceName, "deviceName");
    }

    public static DeviceSelection of(String deviceName) {
        return new DeviceSelection(deviceName);
    }

    public static DeviceSelection gfx() {
        return of("GFX");
    }

    public static DeviceSelection auto() {
        return of("AUTO");
    }

    public String deviceName() {
        return deviceName;
    }

    @Override
    public String toString() {
        return deviceName;
    }
}
