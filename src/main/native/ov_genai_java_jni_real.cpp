#include <jni.h>

#include <filesystem>
#include <fstream>
#include <memory>
#include <mutex>
#include <optional>
#include <set>
#include <sstream>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <utility>
#include <vector>

#include <openvino/core/version.hpp>
#include <openvino/genai/chat_history.hpp>
#include <openvino/genai/generation_config.hpp>
#include <openvino/genai/json_container.hpp>
#include <openvino/genai/llm_pipeline.hpp>
#include <openvino/genai/tokenizer.hpp>
#include <openvino/openvino.hpp>

namespace {

JavaVM* g_jvm = nullptr;

struct PluginRegistrationData {
    std::string device_name;
    std::filesystem::path library_path;
};

std::mutex g_runtime_mutex;
std::vector<PluginRegistrationData> g_plugin_registrations;

struct PipelineHandle {
    explicit PipelineHandle(std::shared_ptr<ov::genai::LLMPipeline> value) : pipeline(std::move(value)) {}
    std::shared_ptr<ov::genai::LLMPipeline> pipeline;
};

struct TokenizerHandle {
    explicit TokenizerHandle(std::shared_ptr<ov::genai::Tokenizer> value) : tokenizer(std::move(value)) {}
    std::shared_ptr<ov::genai::Tokenizer> tokenizer;
};

jclass find_class(JNIEnv* env, const char* name) {
    return env->FindClass(name);
}

void throw_java(JNIEnv* env, const char* class_name, const std::string& message) {
    jclass exception_class = find_class(env, class_name);
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
    }
}

void throw_runtime(JNIEnv* env, const std::string& message) {
    throw_java(env, "java/lang/RuntimeException", message);
}

void throw_illegal_argument(JNIEnv* env, const std::string& message) {
    throw_java(env, "java/lang/IllegalArgumentException", message);
}

void throw_unsupported(JNIEnv* env, const std::string& message) {
    throw_java(env, "java/lang/UnsupportedOperationException", message);
}

std::string jstring_to_string(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error("Unable to read Java string");
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::optional<std::string> optional_jstring_to_string(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return std::nullopt;
    }
    return jstring_to_string(env, value);
}

bool has_pending_exception(JNIEnv* env) {
    return env->ExceptionCheck() == JNI_TRUE;
}

std::string take_pending_exception_message(JNIEnv* env) {
    if (!has_pending_exception(env)) {
        return {};
    }

    jthrowable throwable = env->ExceptionOccurred();
    env->ExceptionClear();
    if (throwable == nullptr) {
        return "Java callback failed";
    }

    jclass throwable_class = env->GetObjectClass(throwable);
    jmethodID to_string = env->GetMethodID(throwable_class, "toString", "()Ljava/lang/String;");
    jstring message = static_cast<jstring>(env->CallObjectMethod(throwable, to_string));
    std::string result = message == nullptr ? "Java callback failed" : jstring_to_string(env, message);

    env->DeleteLocalRef(message);
    env->DeleteLocalRef(throwable_class);
    env->DeleteLocalRef(throwable);
    return result;
}

template <typename T>
T* unwrap_handle(jlong handle) {
    if (handle == 0) {
        throw std::invalid_argument("Native handle is null");
    }
    return reinterpret_cast<T*>(handle);
}

void delete_pipeline_handle(jlong handle) {
    delete unwrap_handle<PipelineHandle>(handle);
}

void delete_tokenizer_handle(jlong handle) {
    delete unwrap_handle<TokenizerHandle>(handle);
}

jobject new_linked_hash_map(JNIEnv* env) {
    jclass map_class = find_class(env, "java/util/LinkedHashMap");
    jmethodID ctor = env->GetMethodID(map_class, "<init>", "()V");
    return env->NewObject(map_class, ctor);
}

jobject new_array_list(JNIEnv* env, jint capacity = 0) {
    jclass list_class = find_class(env, "java/util/ArrayList");
    jmethodID ctor = env->GetMethodID(list_class, "<init>", "(I)V");
    return env->NewObject(list_class, ctor, capacity);
}

void map_put(JNIEnv* env, jobject map, jobject key, jobject value) {
    jclass map_class = env->GetObjectClass(map);
    jmethodID put = env->GetMethodID(map_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject previous = env->CallObjectMethod(map, put, key, value);
    env->DeleteLocalRef(previous);
    env->DeleteLocalRef(map_class);
}

void list_add(JNIEnv* env, jobject list, jobject value) {
    jclass list_class = env->GetObjectClass(list);
    jmethodID add = env->GetMethodID(list_class, "add", "(Ljava/lang/Object;)Z");
    env->CallBooleanMethod(list, add, value);
    env->DeleteLocalRef(list_class);
}

jobject java_long(JNIEnv* env, jlong value) {
    jclass clazz = find_class(env, "java/lang/Long");
    jmethodID value_of = env->GetStaticMethodID(clazz, "valueOf", "(J)Ljava/lang/Long;");
    return env->CallStaticObjectMethod(clazz, value_of, value);
}

jobject java_float(JNIEnv* env, jfloat value) {
    jclass clazz = find_class(env, "java/lang/Float");
    jmethodID value_of = env->GetStaticMethodID(clazz, "valueOf", "(F)Ljava/lang/Float;");
    return env->CallStaticObjectMethod(clazz, value_of, value);
}

jobject java_double(JNIEnv* env, jdouble value) {
    jclass clazz = find_class(env, "java/lang/Double");
    jmethodID value_of = env->GetStaticMethodID(clazz, "valueOf", "(D)Ljava/lang/Double;");
    return env->CallStaticObjectMethod(clazz, value_of, value);
}

jobject java_boolean(JNIEnv* env, jboolean value) {
    jclass clazz = find_class(env, "java/lang/Boolean");
    jmethodID value_of = env->GetStaticMethodID(clazz, "valueOf", "(Z)Ljava/lang/Boolean;");
    return env->CallStaticObjectMethod(clazz, value_of, value);
}

bool is_instance_of(JNIEnv* env, jobject value, const char* class_name) {
    if (value == nullptr) {
        return false;
    }
    jclass clazz = find_class(env, class_name);
    return env->IsInstanceOf(value, clazz) == JNI_TRUE;
}

bool java_boolean_value(JNIEnv* env, jobject value) {
    jclass clazz = env->GetObjectClass(value);
    jmethodID method = env->GetMethodID(clazz, "booleanValue", "()Z");
    jboolean result = env->CallBooleanMethod(value, method);
    env->DeleteLocalRef(clazz);
    return result == JNI_TRUE;
}

jlong java_long_value(JNIEnv* env, jobject value) {
    jclass clazz = env->GetObjectClass(value);
    jmethodID method = env->GetMethodID(clazz, "longValue", "()J");
    jlong result = env->CallLongMethod(value, method);
    env->DeleteLocalRef(clazz);
    return result;
}

jdouble java_double_value(JNIEnv* env, jobject value) {
    jclass clazz = env->GetObjectClass(value);
    jmethodID method = env->GetMethodID(clazz, "doubleValue", "()D");
    jdouble result = env->CallDoubleMethod(value, method);
    env->DeleteLocalRef(clazz);
    return result;
}

bool map_is_empty(JNIEnv* env, jobject map) {
    if (map == nullptr) {
        return true;
    }
    jclass map_class = env->GetObjectClass(map);
    jmethodID is_empty = env->GetMethodID(map_class, "isEmpty", "()Z");
    jboolean result = env->CallBooleanMethod(map, is_empty);
    env->DeleteLocalRef(map_class);
    return result == JNI_TRUE;
}

template <typename Fn>
void for_each_map_entry(JNIEnv* env, jobject map, Fn&& callback) {
    if (map == nullptr) {
        return;
    }

    jclass map_class = env->GetObjectClass(map);
    jmethodID entry_set = env->GetMethodID(map_class, "entrySet", "()Ljava/util/Set;");
    jobject entries = env->CallObjectMethod(map, entry_set);

    jclass set_class = env->GetObjectClass(entries);
    jmethodID iterator_method = env->GetMethodID(set_class, "iterator", "()Ljava/util/Iterator;");
    jobject iterator = env->CallObjectMethod(entries, iterator_method);

    jclass iterator_class = env->GetObjectClass(iterator);
    jmethodID has_next = env->GetMethodID(iterator_class, "hasNext", "()Z");
    jmethodID next = env->GetMethodID(iterator_class, "next", "()Ljava/lang/Object;");

    while (env->CallBooleanMethod(iterator, has_next) == JNI_TRUE) {
        jobject entry = env->CallObjectMethod(iterator, next);
        jclass entry_class = env->GetObjectClass(entry);
        jmethodID get_key = env->GetMethodID(entry_class, "getKey", "()Ljava/lang/Object;");
        jmethodID get_value = env->GetMethodID(entry_class, "getValue", "()Ljava/lang/Object;");

        jobject key_object = env->CallObjectMethod(entry, get_key);
        jobject value_object = env->CallObjectMethod(entry, get_value);

        callback(key_object, value_object);

        env->DeleteLocalRef(value_object);
        env->DeleteLocalRef(key_object);
        env->DeleteLocalRef(entry_class);
        env->DeleteLocalRef(entry);
    }

    env->DeleteLocalRef(iterator_class);
    env->DeleteLocalRef(iterator);
    env->DeleteLocalRef(set_class);
    env->DeleteLocalRef(entries);
    env->DeleteLocalRef(map_class);
}

template <typename T>
std::set<T> collect_number_set(JNIEnv* env, jobject iterable) {
    std::set<T> values;
    jclass iterable_class = env->GetObjectClass(iterable);
    jmethodID iterator_method = env->GetMethodID(iterable_class, "iterator", "()Ljava/util/Iterator;");
    jobject iterator = env->CallObjectMethod(iterable, iterator_method);

    jclass iterator_class = env->GetObjectClass(iterator);
    jmethodID has_next = env->GetMethodID(iterator_class, "hasNext", "()Z");
    jmethodID next = env->GetMethodID(iterator_class, "next", "()Ljava/lang/Object;");

    while (env->CallBooleanMethod(iterator, has_next) == JNI_TRUE) {
        jobject value = env->CallObjectMethod(iterator, next);
        values.insert(static_cast<T>(java_long_value(env, value)));
        env->DeleteLocalRef(value);
    }

    env->DeleteLocalRef(iterator_class);
    env->DeleteLocalRef(iterator);
    env->DeleteLocalRef(iterable_class);
    return values;
}

std::set<std::string> collect_string_set(JNIEnv* env, jobject iterable) {
    std::set<std::string> values;
    jclass iterable_class = env->GetObjectClass(iterable);
    jmethodID iterator_method = env->GetMethodID(iterable_class, "iterator", "()Ljava/util/Iterator;");
    jobject iterator = env->CallObjectMethod(iterable, iterator_method);

    jclass iterator_class = env->GetObjectClass(iterator);
    jmethodID has_next = env->GetMethodID(iterator_class, "hasNext", "()Z");
    jmethodID next = env->GetMethodID(iterator_class, "next", "()Ljava/lang/Object;");

    while (env->CallBooleanMethod(iterator, has_next) == JNI_TRUE) {
        jobject value = env->CallObjectMethod(iterator, next);
        values.insert(jstring_to_string(env, static_cast<jstring>(value)));
        env->DeleteLocalRef(value);
    }

    env->DeleteLocalRef(iterator_class);
    env->DeleteLocalRef(iterator);
    env->DeleteLocalRef(iterable_class);
    return values;
}

ov::AnyMap properties_from_java_map(JNIEnv* env, jobject map) {
    ov::AnyMap properties;
    for_each_map_entry(env, map, [&](jobject key_object, jobject value_object) {
        std::string key = jstring_to_string(env, static_cast<jstring>(key_object));
        if (value_object == nullptr) {
            return;
        }
        if (is_instance_of(env, value_object, "java/lang/String")) {
            properties[key] = jstring_to_string(env, static_cast<jstring>(value_object));
            return;
        }
        if (is_instance_of(env, value_object, "java/lang/Boolean")) {
            properties[key] = java_boolean_value(env, value_object);
            return;
        }
        if (is_instance_of(env, value_object, "java/lang/Float") ||
                is_instance_of(env, value_object, "java/lang/Double")) {
            properties[key] = static_cast<double>(java_double_value(env, value_object));
            return;
        }
        if (is_instance_of(env, value_object, "java/lang/Number")) {
            properties[key] = static_cast<int64_t>(java_long_value(env, value_object));
            return;
        }
        throw std::invalid_argument("Unsupported property type for key: " + key);
    });
    return properties;
}

std::optional<ov::genai::GenerationConfig> generation_config_from_java_map(JNIEnv* env, jobject map) {
    if (map == nullptr || map_is_empty(env, map)) {
        return std::nullopt;
    }

    ov::genai::GenerationConfig config;
    std::optional<std::string> structured_json_schema;
    std::optional<std::string> structured_regex;
    std::optional<std::string> structured_grammar;
    std::optional<std::string> structured_backend;
    for_each_map_entry(env, map, [&](jobject key_object, jobject value_object) {
        std::string key = jstring_to_string(env, static_cast<jstring>(key_object));
        if (value_object == nullptr || key == "return_decoded_results") {
            return;
        }

        auto as_size = [&]() {
            return static_cast<size_t>(java_long_value(env, value_object));
        };
        auto as_bool = [&]() {
            return java_boolean_value(env, value_object);
        };
        auto as_float = [&]() {
            return static_cast<float>(java_double_value(env, value_object));
        };
        auto as_string = [&]() {
            return jstring_to_string(env, static_cast<jstring>(value_object));
        };

        if (key == "max_new_tokens") {
            config.max_new_tokens = as_size();
        } else if (key == "max_length") {
            config.max_length = as_size();
        } else if (key == "ignore_eos") {
            config.ignore_eos = as_bool();
        } else if (key == "min_new_tokens") {
            config.min_new_tokens = as_size();
        } else if (key == "echo") {
            config.echo = as_bool();
        } else if (key == "logprobs") {
            config.logprobs = as_size();
        } else if (key == "eos_token_id") {
            config.eos_token_id = static_cast<int64_t>(java_long_value(env, value_object));
        } else if (key == "stop_strings") {
            config.stop_strings = collect_string_set(env, value_object);
        } else if (key == "include_stop_str_in_output") {
            config.include_stop_str_in_output = as_bool();
        } else if (key == "stop_token_ids") {
            config.stop_token_ids = collect_number_set<int64_t>(env, value_object);
        } else if (key == "repetition_penalty") {
            config.repetition_penalty = as_float();
        } else if (key == "presence_penalty") {
            config.presence_penalty = as_float();
        } else if (key == "frequency_penalty") {
            config.frequency_penalty = as_float();
        } else if (key == "num_beam_groups") {
            config.num_beam_groups = as_size();
        } else if (key == "num_beams") {
            config.num_beams = as_size();
        } else if (key == "diversity_penalty") {
            config.diversity_penalty = as_float();
        } else if (key == "length_penalty") {
            config.length_penalty = as_float();
        } else if (key == "num_return_sequences") {
            config.num_return_sequences = as_size();
        } else if (key == "no_repeat_ngram_size") {
            config.no_repeat_ngram_size = as_size();
        } else if (key == "temperature") {
            config.temperature = as_float();
        } else if (key == "top_p") {
            config.top_p = as_float();
        } else if (key == "top_k") {
            config.top_k = as_size();
        } else if (key == "do_sample") {
            config.do_sample = as_bool();
        } else if (key == "rng_seed") {
            config.rng_seed = as_size();
        } else if (key == "pruning_ratio") {
            config.pruning_ratio = as_size();
        } else if (key == "relevance_weight") {
            config.relevance_weight = as_float();
        } else if (key == "assistant_confidence_threshold") {
            config.assistant_confidence_threshold = as_float();
        } else if (key == "num_assistant_tokens") {
            config.num_assistant_tokens = as_size();
        } else if (key == "max_ngram_size") {
            config.max_ngram_size = as_size();
        } else if (key == "apply_chat_template") {
            config.apply_chat_template = as_bool();
        } else if (key == "json_schema") {
            structured_json_schema = as_string();
        } else if (key == "regex") {
            structured_regex = as_string();
        } else if (key == "grammar") {
            structured_grammar = as_string();
        } else if (key == "backend") {
            structured_backend = as_string();
        } else {
            throw std::invalid_argument("Unsupported generation config key: " + key);
        }
    });

    ov::AnyMap structured_output_properties;
    if (structured_json_schema.has_value()) {
        structured_output_properties["json_schema"] = *structured_json_schema;
    }
    if (structured_regex.has_value()) {
        structured_output_properties["regex"] = *structured_regex;
    }
    if (structured_grammar.has_value()) {
        structured_output_properties["grammar"] = *structured_grammar;
    }
    if (structured_backend.has_value()) {
        structured_output_properties["backend"] = *structured_backend;
    }
    if (!structured_output_properties.empty()) {
        config.structured_output_config = ov::genai::StructuredOutputConfig(structured_output_properties);
    }

    config.validate();
    return config;
}

void put_map_value(JNIEnv* env, jobject map, const char* key, jobject value) {
    jstring key_object = env->NewStringUTF(key);
    map_put(env, map, key_object, value);
    env->DeleteLocalRef(key_object);
    env->DeleteLocalRef(value);
}

jobject generation_config_to_java_map(JNIEnv* env, const ov::genai::GenerationConfig& config) {
    jobject map = new_linked_hash_map(env);
    put_map_value(env, map, "max_new_tokens", java_long(env, static_cast<jlong>(config.max_new_tokens)));
    put_map_value(env, map, "max_length", java_long(env, static_cast<jlong>(config.max_length)));
    put_map_value(env, map, "ignore_eos", java_boolean(env, config.ignore_eos ? JNI_TRUE : JNI_FALSE));
    put_map_value(env, map, "min_new_tokens", java_long(env, static_cast<jlong>(config.min_new_tokens)));
    put_map_value(env, map, "echo", java_boolean(env, config.echo ? JNI_TRUE : JNI_FALSE));
    put_map_value(env, map, "logprobs", java_long(env, static_cast<jlong>(config.logprobs)));
    put_map_value(env, map, "eos_token_id", java_long(env, static_cast<jlong>(config.eos_token_id)));
    put_map_value(env, map, "repetition_penalty", java_double(env, config.repetition_penalty));
    put_map_value(env, map, "presence_penalty", java_double(env, config.presence_penalty));
    put_map_value(env, map, "frequency_penalty", java_double(env, config.frequency_penalty));
    put_map_value(env, map, "num_beam_groups", java_long(env, static_cast<jlong>(config.num_beam_groups)));
    put_map_value(env, map, "num_beams", java_long(env, static_cast<jlong>(config.num_beams)));
    put_map_value(env, map, "diversity_penalty", java_double(env, config.diversity_penalty));
    put_map_value(env, map, "length_penalty", java_double(env, config.length_penalty));
    put_map_value(env, map, "num_return_sequences", java_long(env, static_cast<jlong>(config.num_return_sequences)));
    put_map_value(env, map, "no_repeat_ngram_size", java_long(env, static_cast<jlong>(config.no_repeat_ngram_size)));
    put_map_value(env, map, "temperature", java_double(env, config.temperature));
    put_map_value(env, map, "top_p", java_double(env, config.top_p));
    put_map_value(env, map, "top_k", java_long(env, static_cast<jlong>(config.top_k)));
    put_map_value(env, map, "do_sample", java_boolean(env, config.do_sample ? JNI_TRUE : JNI_FALSE));
    put_map_value(env, map, "rng_seed", java_long(env, static_cast<jlong>(config.rng_seed)));
    put_map_value(env, map, "pruning_ratio", java_long(env, static_cast<jlong>(config.pruning_ratio)));
    put_map_value(env, map, "relevance_weight", java_double(env, config.relevance_weight));
    put_map_value(env, map, "assistant_confidence_threshold", java_double(env, config.assistant_confidence_threshold));
    put_map_value(env, map, "num_assistant_tokens", java_long(env, static_cast<jlong>(config.num_assistant_tokens)));
    put_map_value(env, map, "max_ngram_size", java_long(env, static_cast<jlong>(config.max_ngram_size)));
    put_map_value(env, map, "apply_chat_template", java_boolean(env, config.apply_chat_template ? JNI_TRUE : JNI_FALSE));

    if (config.structured_output_config.has_value()) {
        const auto& structured = *config.structured_output_config;
        if (structured.json_schema.has_value()) {
            put_map_value(env, map, "json_schema", env->NewStringUTF(structured.json_schema->c_str()));
        }
        if (structured.regex.has_value()) {
            put_map_value(env, map, "regex", env->NewStringUTF(structured.regex->c_str()));
        }
        if (structured.grammar.has_value()) {
            put_map_value(env, map, "grammar", env->NewStringUTF(structured.grammar->c_str()));
        }
        if (structured.backend.has_value()) {
            put_map_value(env, map, "backend", env->NewStringUTF(structured.backend->c_str()));
        }
    }

    return map;
}

jobject perf_metrics_to_java_map(JNIEnv* env, const ov::genai::PerfMetrics& perf_metrics) {
    jobject map = new_linked_hash_map(env);
    put_map_value(env, map, "load_time_ms", java_double(env, perf_metrics.load_time));
    put_map_value(env, map, "ttft_mean_ms", java_double(env, perf_metrics.ttft.mean));
    put_map_value(env, map, "ttft_std_ms", java_double(env, perf_metrics.ttft.std));
    put_map_value(env, map, "tpot_mean_ms", java_double(env, perf_metrics.tpot.mean));
    put_map_value(env, map, "tpot_std_ms", java_double(env, perf_metrics.tpot.std));
    put_map_value(env, map, "ipot_mean_ms", java_double(env, perf_metrics.ipot.mean));
    put_map_value(env, map, "ipot_std_ms", java_double(env, perf_metrics.ipot.std));
    put_map_value(env, map, "throughput_mean_tps", java_double(env, perf_metrics.throughput.mean));
    put_map_value(env, map, "throughput_std_tps", java_double(env, perf_metrics.throughput.std));
    put_map_value(env, map, "generate_duration_mean_ms", java_double(env, perf_metrics.generate_duration.mean));
    put_map_value(env, map, "generate_duration_std_ms", java_double(env, perf_metrics.generate_duration.std));
    put_map_value(env, map, "inference_duration_mean_ms", java_double(env, perf_metrics.inference_duration.mean));
    put_map_value(env, map, "inference_duration_std_ms", java_double(env, perf_metrics.inference_duration.std));
    put_map_value(env, map, "tokenization_duration_mean_ms", java_double(env, perf_metrics.tokenization_duration.mean));
    put_map_value(env, map, "tokenization_duration_std_ms", java_double(env, perf_metrics.tokenization_duration.std));
    put_map_value(env, map, "detokenization_duration_mean_ms", java_double(env, perf_metrics.detokenization_duration.mean));
    put_map_value(env, map, "detokenization_duration_std_ms", java_double(env, perf_metrics.detokenization_duration.std));
    put_map_value(env, map, "num_generated_tokens", java_long(env, static_cast<jlong>(perf_metrics.num_generated_tokens)));
    put_map_value(env, map, "num_input_tokens", java_long(env, static_cast<jlong>(perf_metrics.num_input_tokens)));
    return map;
}

jobject generation_status_object(JNIEnv* env, jint native_value) {
    jclass status_class = find_class(env, "com/ovx/openvino/genai/GenerationStatus");
    jmethodID from_native = env->GetStaticMethodID(
            status_class,
            "fromNativeValue",
            "(I)Lcom/ovx/openvino/genai/GenerationStatus;");
    return env->CallStaticObjectMethod(status_class, from_native, native_value);
}

jobject decoded_results_to_java(JNIEnv* env, const ov::genai::DecodedResults& results, jint status_value) {
    jobject texts = new_array_list(env, static_cast<jint>(results.texts.size()));
    for (const auto& text : results.texts) {
        jstring text_object = env->NewStringUTF(text.c_str());
        list_add(env, texts, text_object);
        env->DeleteLocalRef(text_object);
    }

    jobject scores = new_array_list(env, static_cast<jint>(results.scores.size()));
    for (float score : results.scores) {
        jobject score_object = java_float(env, score);
        list_add(env, scores, score_object);
        env->DeleteLocalRef(score_object);
    }

    jobject parsed = new_array_list(env, static_cast<jint>(results.parsed.size()));
    for (const auto& item : results.parsed) {
        std::string json = item.to_json_string();
        jstring json_object = env->NewStringUTF(json.c_str());
        list_add(env, parsed, json_object);
        env->DeleteLocalRef(json_object);
    }

    jobject perf_map = perf_metrics_to_java_map(env, results.perf_metrics);
    jobject extended_perf_map = results.extended_perf_metrics == nullptr
            ? new_linked_hash_map(env)
            : perf_metrics_to_java_map(env, *results.extended_perf_metrics);
    jobject status = generation_status_object(env, status_value);

    jclass result_class = find_class(env, "com/ovx/openvino/genai/GenerationResult");
    jmethodID ctor = env->GetMethodID(
            result_class,
            "<init>",
            "(JLjava/util/List;Ljava/util/List;Lcom/ovx/openvino/genai/GenerationStatus;Ljava/util/Map;Ljava/util/Map;Ljava/util/List;)V");

    jobject result = env->NewObject(
            result_class,
            ctor,
            static_cast<jlong>(0),
            texts,
            scores,
            status,
            perf_map,
            extended_perf_map,
            parsed);

    env->DeleteLocalRef(status);
    env->DeleteLocalRef(extended_perf_map);
    env->DeleteLocalRef(perf_map);
    env->DeleteLocalRef(parsed);
    env->DeleteLocalRef(scores);
    env->DeleteLocalRef(texts);
    return result;
}

ov::genai::ChatHistory chat_history_from_json(const std::string& json) {
    ov::genai::JsonContainer root = ov::genai::JsonContainer::from_json_string(json);
    ov::genai::ChatHistory history(root["messages"]);
    if (root.contains("tools")) {
        history.set_tools(root["tools"]);
    }
    if (root.contains("extra_context")) {
        history.set_extra_context(root["extra_context"]);
    }
    return history;
}

std::string xml_escape(const std::string& value) {
    std::string result;
    result.reserve(value.size());
    for (char ch : value) {
        switch (ch) {
            case '&':
                result += "&amp;";
                break;
            case '"':
                result += "&quot;";
                break;
            case '\'':
                result += "&apos;";
                break;
            case '<':
                result += "&lt;";
                break;
            case '>':
                result += "&gt;";
                break;
            default:
                result.push_back(ch);
                break;
        }
    }
    return result;
}

void write_plugins_xml_if_possible() {
    if (g_plugin_registrations.empty()) {
        return;
    }

    const auto common_parent = g_plugin_registrations.front().library_path.parent_path();
    if (common_parent.empty()) {
        return;
    }

    for (const auto& registration : g_plugin_registrations) {
        if (registration.library_path.parent_path() != common_parent) {
            return;
        }
    }

    const auto xml_path = common_parent / "plugins.xml";
    std::ofstream stream(xml_path, std::ios::trunc);
    if (!stream) {
        return;
    }

    stream << "<ie>\n";
    stream << "    <plugins>\n";
    for (const auto& registration : g_plugin_registrations) {
        stream << "        <plugin name=\"" << xml_escape(registration.device_name)
               << "\" location=\"" << xml_escape(registration.library_path.filename().string()) << "\">\n";
        stream << "        </plugin>\n";
    }
    stream << "    </plugins>\n";
    stream << "</ie>\n";
}

class ScopedEnv {
public:
    explicit ScopedEnv(JavaVM* jvm) : m_jvm(jvm) {
        if (m_jvm == nullptr) {
            return;
        }

        if (m_jvm->GetEnv(reinterpret_cast<void**>(&m_env), JNI_VERSION_1_6) == JNI_OK) {
            return;
        }

        if (m_jvm->AttachCurrentThread(&m_env, nullptr) == JNI_OK) {
            m_attached = true;
        }
    }

    ~ScopedEnv() {
        if (m_attached && m_jvm != nullptr) {
            m_jvm->DetachCurrentThread();
        }
    }

    JNIEnv* get() const {
        return m_env;
    }

private:
    JavaVM* m_jvm = nullptr;
    JNIEnv* m_env = nullptr;
    bool m_attached = false;
};

class StreamingBridge {
public:
    StreamingBridge(JNIEnv* env, jobject callback)
        : m_callback(env->NewGlobalRef(callback)) {
        jclass callback_class = env->GetObjectClass(callback);
        m_on_text = env->GetMethodID(
                callback_class,
                "onText",
                "(Ljava/lang/String;)Lcom/ovx/openvino/genai/StreamingStatus;");
        env->DeleteLocalRef(callback_class);

        jclass status_class = find_class(env, "com/ovx/openvino/genai/StreamingStatus");
        m_status_class = reinterpret_cast<jclass>(env->NewGlobalRef(status_class));
        m_native_value = env->GetMethodID(m_status_class, "nativeValue", "()I");
        env->DeleteLocalRef(status_class);
    }

    ~StreamingBridge() {
        ScopedEnv scoped(g_jvm);
        if (JNIEnv* env = scoped.get()) {
            env->DeleteGlobalRef(m_status_class);
            env->DeleteGlobalRef(m_callback);
        }
    }

    ov::genai::StreamingStatus operator()(std::string text) {
        ScopedEnv scoped(g_jvm);
        JNIEnv* env = scoped.get();
        if (env == nullptr) {
            record_error("Unable to attach native streaming thread to JVM");
            return ov::genai::StreamingStatus::CANCEL;
        }

        jstring chunk = env->NewStringUTF(text.c_str());
        jobject status = env->CallObjectMethod(m_callback, m_on_text, chunk);
        env->DeleteLocalRef(chunk);

        if (has_pending_exception(env)) {
            record_error(take_pending_exception_message(env));
            if (status != nullptr) {
                env->DeleteLocalRef(status);
            }
            return ov::genai::StreamingStatus::CANCEL;
        }

        if (status == nullptr) {
            m_last_status = ov::genai::StreamingStatus::CANCEL;
            return *m_last_status;
        }

        jint code = env->CallIntMethod(status, m_native_value);
        env->DeleteLocalRef(status);

        if (has_pending_exception(env)) {
            record_error(take_pending_exception_message(env));
            return ov::genai::StreamingStatus::CANCEL;
        }

        switch (code) {
            case 1:
                m_last_status = ov::genai::StreamingStatus::STOP;
                return *m_last_status;
            case 2:
                m_last_status = ov::genai::StreamingStatus::CANCEL;
                return *m_last_status;
            case 0:
            default:
                m_last_status = ov::genai::StreamingStatus::RUNNING;
                return *m_last_status;
        }
    }

    std::optional<ov::genai::StreamingStatus> last_status() const {
        return m_last_status;
    }

    std::optional<std::string> error_message() const {
        std::scoped_lock lock(m_mutex);
        return m_error;
    }

private:
    void record_error(std::string message) {
        std::scoped_lock lock(m_mutex);
        m_error = std::move(message);
    }

    jobject m_callback = nullptr;
    jclass m_status_class = nullptr;
    jmethodID m_on_text = nullptr;
    jmethodID m_native_value = nullptr;
    mutable std::mutex m_mutex;
    std::optional<std::string> m_error;
    std::optional<ov::genai::StreamingStatus> m_last_status;
};

jint normal_generation_status(const std::optional<ov::genai::StreamingStatus>& status) {
    if (!status.has_value()) {
        return 1;
    }
    switch (*status) {
        case ov::genai::StreamingStatus::STOP:
            return 4;
        case ov::genai::StreamingStatus::CANCEL:
            return 3;
        case ov::genai::StreamingStatus::RUNNING:
        default:
            return 1;
    }
}

template <typename Fn>
auto run_jni(JNIEnv* env, Fn&& fn) -> decltype(fn()) {
    try {
        return fn();
    } catch (const std::invalid_argument& error) {
        throw_illegal_argument(env, error.what());
    } catch (const ov::Exception& error) {
        throw_runtime(env, error.what());
    } catch (const std::exception& error) {
        throw_runtime(env, error.what());
    } catch (...) {
        throw_runtime(env, "Unexpected native exception");
    }
    using ReturnType = decltype(fn());
    if constexpr (!std::is_void_v<ReturnType>) {
        return ReturnType();
    }
}

}  // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nRuntimeVersion(JNIEnv* env, jclass) {
    return run_jni(env, [&]() -> jstring {
        const auto version = ov::get_openvino_version();
        std::string value = "OpenVINO " + std::string(version.buildNumber);
        return env->NewStringUTF(value.c_str());
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nRuntimeConfigure(
        JNIEnv* env,
        jclass,
        jobjectArray device_names,
        jobjectArray library_paths,
        jobjectArray) {
    run_jni(env, [&]() {
        std::vector<PluginRegistrationData> registrations;
        const auto count = env->GetArrayLength(device_names);
        if (count != env->GetArrayLength(library_paths)) {
            throw std::invalid_argument("Plugin registration arrays must have equal length");
        }

        registrations.reserve(static_cast<size_t>(count));
        for (jsize i = 0; i < count; ++i) {
            auto device_name = static_cast<jstring>(env->GetObjectArrayElement(device_names, i));
            auto library_path = static_cast<jstring>(env->GetObjectArrayElement(library_paths, i));

            registrations.push_back(PluginRegistrationData{
                    jstring_to_string(env, device_name),
                    std::filesystem::path(jstring_to_string(env, library_path))});

            env->DeleteLocalRef(library_path);
            env->DeleteLocalRef(device_name);
        }

        std::scoped_lock lock(g_runtime_mutex);
        g_plugin_registrations = std::move(registrations);
        write_plugins_xml_if_possible();
    });
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmCreate(
        JNIEnv* env,
        jclass,
        jstring model_path,
        jstring device,
        jobject properties) {
    return run_jni(env, [&]() -> jlong {
        auto pipeline = std::make_shared<ov::genai::LLMPipeline>(
                std::filesystem::path(jstring_to_string(env, model_path)),
                jstring_to_string(env, device),
                properties_from_java_map(env, properties));
        return reinterpret_cast<jlong>(new PipelineHandle(std::move(pipeline)));
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmDispose(JNIEnv* env, jclass, jlong handle) {
    run_jni(env, [&]() {
        delete_pipeline_handle(handle);
    });
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGenerate(
        JNIEnv* env,
        jclass,
        jlong handle,
        jstring prompt,
        jobject generation_config,
        jobject callback) {
    return run_jni(env, [&]() -> jobject {
        auto* pipeline_handle = unwrap_handle<PipelineHandle>(handle);
        auto prompt_text = jstring_to_string(env, prompt);
        auto config = generation_config_from_java_map(env, generation_config);

        if (callback == nullptr) {
            auto results = pipeline_handle->pipeline->generate(prompt_text, config, std::monostate());
            return decoded_results_to_java(env, results, 1);
        }

        StreamingBridge bridge(env, callback);
        auto streamer = [&](std::string chunk) {
            return bridge(std::move(chunk));
        };
        auto results = pipeline_handle->pipeline->generate(prompt_text, config, streamer);
        if (bridge.error_message().has_value()) {
            throw std::runtime_error(*bridge.error_message());
        }
        return decoded_results_to_java(env, results, normal_generation_status(bridge.last_status()));
    });
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGenerateChat(
        JNIEnv* env,
        jclass,
        jlong handle,
        jstring history_json,
        jobject generation_config,
        jobject callback) {
    return run_jni(env, [&]() -> jobject {
        auto* pipeline_handle = unwrap_handle<PipelineHandle>(handle);
        auto history = chat_history_from_json(jstring_to_string(env, history_json));
        auto config = generation_config_from_java_map(env, generation_config);

        if (callback == nullptr) {
            auto results = pipeline_handle->pipeline->generate(history, config, std::monostate());
            return decoded_results_to_java(env, results, 1);
        }

        StreamingBridge bridge(env, callback);
        auto streamer = [&](std::string chunk) {
            return bridge(std::move(chunk));
        };
        auto results = pipeline_handle->pipeline->generate(history, config, streamer);
        if (bridge.error_message().has_value()) {
            throw std::runtime_error(*bridge.error_message());
        }
        return decoded_results_to_java(env, results, normal_generation_status(bridge.last_status()));
    });
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGetGenerationConfig(
        JNIEnv* env,
        jclass,
        jlong handle) {
    return run_jni(env, [&]() -> jobject {
        auto* pipeline_handle = unwrap_handle<PipelineHandle>(handle);
        return generation_config_to_java_map(env, pipeline_handle->pipeline->get_generation_config());
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmSetGenerationConfig(
        JNIEnv* env,
        jclass,
        jlong handle,
        jobject generation_config) {
    run_jni(env, [&]() {
        auto* pipeline_handle = unwrap_handle<PipelineHandle>(handle);
        auto config = generation_config_from_java_map(env, generation_config);
        pipeline_handle->pipeline->set_generation_config(
                config.value_or(ov::genai::GenerationConfig{}));
    });
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nLlmGetTokenizer(
        JNIEnv* env,
        jclass,
        jlong handle) {
    return run_jni(env, [&]() -> jlong {
        auto* pipeline_handle = unwrap_handle<PipelineHandle>(handle);
        auto tokenizer = std::make_shared<ov::genai::Tokenizer>(pipeline_handle->pipeline->get_tokenizer());
        return reinterpret_cast<jlong>(new TokenizerHandle(std::move(tokenizer)));
    });
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbCreate(
        JNIEnv* env,
        jclass,
        jstring,
        jstring,
        jobject,
        jobject) {
    throw_unsupported(env, "Continuous batching JNI bridge is not implemented yet");
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
    throw_unsupported(env, "Continuous batching JNI bridge is not implemented yet");
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbStep(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Continuous batching JNI bridge is not implemented yet");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbHasNonFinishedRequests(
        JNIEnv* env,
        jclass,
        jlong) {
    throw_unsupported(env, "Continuous batching JNI bridge is not implemented yet");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nCbGetMetrics(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "Continuous batching JNI bridge is not implemented yet");
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleDispose(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleRead(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "GenerationHandle JNI bridge is not implemented yet");
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleReadAll(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "GenerationHandle JNI bridge is not implemented yet");
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleGetStatus(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "GenerationHandle JNI bridge is not implemented yet");
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleStop(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "GenerationHandle JNI bridge is not implemented yet");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nHandleCancel(JNIEnv* env, jclass, jlong) {
    throw_unsupported(env, "GenerationHandle JNI bridge is not implemented yet");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nTokenizerDispose(JNIEnv* env, jclass, jlong handle) {
    run_jni(env, [&]() {
        delete_tokenizer_handle(handle);
    });
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ovx_openvino_genai_internal_NativeBindings_nTokenizerApplyChatTemplate(
        JNIEnv* env,
        jclass,
        jlong handle,
        jstring history_json,
        jboolean add_generation_prompt,
        jstring chat_template,
        jstring tools_json,
        jstring extra_context_json) {
    return run_jni(env, [&]() -> jstring {
        auto* tokenizer_handle = unwrap_handle<TokenizerHandle>(handle);
        auto history = chat_history_from_json(jstring_to_string(env, history_json));

        std::optional<ov::genai::JsonContainer> tools = std::nullopt;
        if (auto tools_text = optional_jstring_to_string(env, tools_json); tools_text.has_value()) {
            tools = ov::genai::JsonContainer::from_json_string(*tools_text);
        }

        std::optional<ov::genai::JsonContainer> extra_context = std::nullopt;
        if (auto extra_context_text = optional_jstring_to_string(env, extra_context_json);
                extra_context_text.has_value()) {
            extra_context = ov::genai::JsonContainer::from_json_string(*extra_context_text);
        }

        std::string result = tokenizer_handle->tokenizer->apply_chat_template(
                history,
                add_generation_prompt == JNI_TRUE,
                optional_jstring_to_string(env, chat_template).value_or(""),
                tools,
                extra_context);
        return env->NewStringUTF(result.c_str());
    });
}
