package com.ovx.openvino.genai;

import com.ovx.openvino.genai.internal.JsonSupport;

import java.util.Objects;

public final class ChatMessage {
    private final ChatRole role;
    private final String content;
    private final String name;

    private ChatMessage(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.name = builder.name;
    }

    public static Builder builder(ChatRole role, String content) {
        return new Builder(role, content);
    }

    public static ChatMessage system(String content) {
        return builder(ChatRole.SYSTEM, content).build();
    }

    public static ChatMessage user(String content) {
        return builder(ChatRole.USER, content).build();
    }

    public static ChatMessage assistant(String content) {
        return builder(ChatRole.ASSISTANT, content).build();
    }

    public ChatRole role() {
        return role;
    }

    public String content() {
        return content;
    }

    public String name() {
        return name;
    }

    String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
                .append("\"role\":\"").append(JsonSupport.escape(role.wireValue())).append('"')
                .append(",\"content\":\"").append(JsonSupport.escape(content)).append('"');
        if (name != null) {
            builder.append(",\"name\":\"").append(JsonSupport.escape(name)).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    public static final class Builder {
        private final ChatRole role;
        private final String content;
        private String name;

        private Builder(ChatRole role, String content) {
            this.role = Objects.requireNonNull(role, "role");
            this.content = Objects.requireNonNull(content, "content");
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public ChatMessage build() {
            return new ChatMessage(this);
        }
    }
}
