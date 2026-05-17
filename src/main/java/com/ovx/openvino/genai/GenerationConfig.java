package com.ovx.openvino.genai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GenerationConfig {
    private final Map<String, Object> properties;

    private GenerationConfig(Builder builder) {
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GenerationConfig empty() {
        return builder().build();
    }

    public static GenerationConfig fromMap(Map<String, Object> properties) {
        Builder builder = builder();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public Map<String, Object> toMap() {
        return properties;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(GenerationConfig config) {
            properties.putAll(config.properties);
        }

        public Builder put(String name, Object value) {
            properties.put(Objects.requireNonNull(name, "name"), value);
            return this;
        }

        public Builder maxNewTokens(long value) {
            return put("max_new_tokens", value);
        }

        public Builder doSample(boolean value) {
            return put("do_sample", value);
        }

        public Builder topP(double value) {
            return put("top_p", value);
        }

        public Builder topK(long value) {
            return put("top_k", value);
        }

        public Builder temperature(double value) {
            return put("temperature", value);
        }

        public Builder numBeams(long value) {
            return put("num_beams", value);
        }

        public Builder numBeamGroups(long value) {
            return put("num_beam_groups", value);
        }

        public Builder diversityPenalty(double value) {
            return put("diversity_penalty", value);
        }

        public Builder applyChatTemplate(boolean value) {
            return put("apply_chat_template", value);
        }

        public Builder returnDecodedResults(boolean value) {
            return put("return_decoded_results", value);
        }

        public Builder jsonSchema(String jsonSchema) {
            return put("json_schema", Objects.requireNonNull(jsonSchema, "jsonSchema"));
        }

        public Builder regex(String regex) {
            return put("regex", Objects.requireNonNull(regex, "regex"));
        }

        public Builder grammar(String grammar) {
            return put("grammar", Objects.requireNonNull(grammar, "grammar"));
        }

        public GenerationConfig build() {
            return new GenerationConfig(this);
        }
    }
}
