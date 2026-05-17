package com.ovx.openvino.genai.internal;

import com.ovx.openvino.genai.GenerationConfig;
import com.ovx.openvino.genai.GenerationResult;
import com.ovx.openvino.genai.PipelineMetrics;
import com.ovx.openvino.genai.PluginRegistration;
import com.ovx.openvino.genai.StreamingCallback;

import java.util.List;
import java.util.Map;

public final class NativeBindings {
    private NativeBindings() {
    }

    public static String runtimeVersion() {
        NativeLoader.ensureLoaded();
        return nRuntimeVersion();
    }

    public static void runtimeConfigure(List<PluginRegistration> plugins) {
        NativeLoader.ensureLoaded();
        String[] deviceNames = new String[plugins.size()];
        String[] libraryPaths = new String[plugins.size()];
        String[] propertyJson = new String[plugins.size()];
        for (int i = 0; i < plugins.size(); i++) {
            PluginRegistration plugin = plugins.get(i);
            deviceNames[i] = plugin.deviceName();
            libraryPaths[i] = plugin.libraryPath();
            propertyJson[i] = JsonSupport.toJson(plugin.properties());
        }
        nRuntimeConfigure(deviceNames, libraryPaths, propertyJson);
    }

    public static long llmCreate(String modelPath, String device, Map<String, Object> properties) {
        NativeLoader.ensureLoaded();
        return nLlmCreate(modelPath, device, properties);
    }

    public static void llmDispose(long handle) {
        NativeLoader.ensureLoaded();
        nLlmDispose(handle);
    }

    public static GenerationResult llmGenerate(
            long handle,
            String prompt,
            Map<String, Object> generationConfig,
            StreamingCallback callback) {
        NativeLoader.ensureLoaded();
        return nLlmGenerate(handle, prompt, generationConfig, callback);
    }

    public static GenerationResult llmGenerateChat(
            long handle,
            String chatHistoryJson,
            Map<String, Object> generationConfig,
            StreamingCallback callback) {
        NativeLoader.ensureLoaded();
        return nLlmGenerateChat(handle, chatHistoryJson, generationConfig, callback);
    }

    public static Map<String, Object> llmGetGenerationConfig(long handle) {
        NativeLoader.ensureLoaded();
        return nLlmGetGenerationConfig(handle);
    }

    public static void llmSetGenerationConfig(long handle, Map<String, Object> generationConfig) {
        NativeLoader.ensureLoaded();
        nLlmSetGenerationConfig(handle, generationConfig);
    }

    public static long llmGetTokenizer(long handle) {
        NativeLoader.ensureLoaded();
        return nLlmGetTokenizer(handle);
    }

    public static long cbCreate(
            String modelPath,
            String device,
            Map<String, Object> schedulerConfig,
            Map<String, Object> properties) {
        NativeLoader.ensureLoaded();
        return nCbCreate(modelPath, device, schedulerConfig, properties);
    }

    public static void cbDispose(long handle) {
        NativeLoader.ensureLoaded();
        nCbDispose(handle);
    }

    public static long cbAddRequest(
            long handle,
            long requestId,
            String prompt,
            Map<String, Object> generationConfig) {
        NativeLoader.ensureLoaded();
        return nCbAddRequest(handle, requestId, prompt, generationConfig);
    }

    public static void cbStep(long handle) {
        NativeLoader.ensureLoaded();
        nCbStep(handle);
    }

    public static boolean cbHasNonFinishedRequests(long handle) {
        NativeLoader.ensureLoaded();
        return nCbHasNonFinishedRequests(handle);
    }

    public static PipelineMetrics cbGetMetrics(long handle) {
        NativeLoader.ensureLoaded();
        return nCbGetMetrics(handle);
    }

    public static void handleDispose(long handle) {
        NativeLoader.ensureLoaded();
        nHandleDispose(handle);
    }

    public static GenerationResult handleRead(long handle) {
        NativeLoader.ensureLoaded();
        return nHandleRead(handle);
    }

    public static GenerationResult handleReadAll(long handle) {
        NativeLoader.ensureLoaded();
        return nHandleReadAll(handle);
    }

    public static int handleGetStatus(long handle) {
        NativeLoader.ensureLoaded();
        return nHandleGetStatus(handle);
    }

    public static void handleStop(long handle) {
        NativeLoader.ensureLoaded();
        nHandleStop(handle);
    }

    public static void handleCancel(long handle) {
        NativeLoader.ensureLoaded();
        nHandleCancel(handle);
    }

    public static void tokenizerDispose(long handle) {
        NativeLoader.ensureLoaded();
        nTokenizerDispose(handle);
    }

    public static String tokenizerApplyChatTemplate(
            long handle,
            String historyJson,
            boolean addGenerationPrompt,
            String chatTemplate,
            String toolsJson,
            String extraContextJson) {
        NativeLoader.ensureLoaded();
        return nTokenizerApplyChatTemplate(
                handle,
                historyJson,
                addGenerationPrompt,
                chatTemplate,
                toolsJson,
                extraContextJson);
    }

    private static native String nRuntimeVersion();

    private static native void nRuntimeConfigure(
            String[] deviceNames,
            String[] libraryPaths,
            String[] propertyJson);

    private static native long nLlmCreate(
            String modelPath,
            String device,
            Map<String, Object> properties);

    private static native void nLlmDispose(long handle);

    private static native GenerationResult nLlmGenerate(
            long handle,
            String prompt,
            Map<String, Object> generationConfig,
            StreamingCallback callback);

    private static native GenerationResult nLlmGenerateChat(
            long handle,
            String historyJson,
            Map<String, Object> generationConfig,
            StreamingCallback callback);

    private static native Map<String, Object> nLlmGetGenerationConfig(long handle);

    private static native void nLlmSetGenerationConfig(
            long handle,
            Map<String, Object> generationConfig);

    private static native long nLlmGetTokenizer(long handle);

    private static native long nCbCreate(
            String modelPath,
            String device,
            Map<String, Object> schedulerConfig,
            Map<String, Object> properties);

    private static native void nCbDispose(long handle);

    private static native long nCbAddRequest(
            long handle,
            long requestId,
            String prompt,
            Map<String, Object> generationConfig);

    private static native void nCbStep(long handle);

    private static native boolean nCbHasNonFinishedRequests(long handle);

    private static native PipelineMetrics nCbGetMetrics(long handle);

    private static native void nHandleDispose(long handle);

    private static native GenerationResult nHandleRead(long handle);

    private static native GenerationResult nHandleReadAll(long handle);

    private static native int nHandleGetStatus(long handle);

    private static native void nHandleStop(long handle);

    private static native void nHandleCancel(long handle);

    private static native void nTokenizerDispose(long handle);

    private static native String nTokenizerApplyChatTemplate(
            long handle,
            String historyJson,
            boolean addGenerationPrompt,
            String chatTemplate,
            String toolsJson,
            String extraContextJson);
}
