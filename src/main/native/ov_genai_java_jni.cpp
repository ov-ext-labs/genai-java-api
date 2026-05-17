#include <jni.h>

namespace {

jclass find_class(JNIEnv* env, const char* name) {
    return env->FindClass(name);
}

void throw_unsupported(JNIEnv* env, const char* message) {
    jclass exception_class = find_class(env, "java/lang/UnsupportedOperationException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message);
    }
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nRuntimeVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF("ov_genai_java_jni_stub");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nRuntimeConfigure(
        JNIEnv*,
        jclass,
        jobjectArray,
        jobjectArray,
        jobjectArray) {
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmCreate(
        JNIEnv* env,
        jclass,
        jstring,
        jstring,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmDispose(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGenerate(
        JNIEnv* env,
        jclass,
        jlong,
        jstring,
        jobject,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGenerateChat(
        JNIEnv* env,
        jclass,
        jlong,
        jstring,
        jobject,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGetGenerationConfig(
        JNIEnv* env,
        jclass,
        jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmSetGenerationConfig(
        JNIEnv* env,
        jclass,
        jlong,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGetTokenizer(
        JNIEnv* env,
        jclass,
        jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbCreate(
        JNIEnv* env,
        jclass,
        jstring,
        jstring,
        jobject,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbDispose(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbAddRequest(
        JNIEnv* env,
        jclass,
        jlong,
        jlong,
        jstring,
        jobject) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbStep(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbHasNonFinishedRequests(
        JNIEnv* env,
        jclass,
        jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbGetMetrics(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleDispose(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleRead(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleReadAll(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleGetStatus(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleStop(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleCancel(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nTokenizerDispose(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nTokenizerApplyChatTemplate(
        JNIEnv* env,
        jclass,
        jlong,
        jstring,
        jboolean,
        jstring,
        jstring,
        jstring) {
    throw_unsupported(env, "Real OpenVINO GenAI native bridge is not linked yet");
    return nullptr;
}
