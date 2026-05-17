package com.ovx.openvino.genai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChatHistory {
    private final List<ChatMessage> messages;
    private final String toolsJson;
    private final String extraContextJson;

    private ChatHistory(Builder builder) {
        this.messages = List.copyOf(builder.messages);
        this.toolsJson = builder.toolsJson;
        this.extraContextJson = builder.extraContextJson;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public String toolsJson() {
        return toolsJson;
    }

    public String extraContextJson() {
        return extraContextJson;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append('{').append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(messages.get(i).toJson());
        }
        builder.append(']');
        if (toolsJson != null) {
            builder.append(",\"tools\":").append(toolsJson);
        }
        if (extraContextJson != null) {
            builder.append(",\"extra_context\":").append(extraContextJson);
        }
        builder.append('}');
        return builder.toString();
    }

    public static final class Builder {
        private final List<ChatMessage> messages = new ArrayList<>();
        private String toolsJson;
        private String extraContextJson;

        public Builder add(ChatMessage message) {
            messages.add(Objects.requireNonNull(message, "message"));
            return this;
        }

        public Builder addSystem(String content) {
            return add(ChatMessage.system(content));
        }

        public Builder addUser(String content) {
            return add(ChatMessage.user(content));
        }

        public Builder addAssistant(String content) {
            return add(ChatMessage.assistant(content));
        }

        public Builder toolsJson(String toolsJson) {
            this.toolsJson = Objects.requireNonNull(toolsJson, "toolsJson");
            return this;
        }

        public Builder extraContextJson(String extraContextJson) {
            this.extraContextJson = Objects.requireNonNull(extraContextJson, "extraContextJson");
            return this;
        }

        public ChatHistory build() {
            return new ChatHistory(this);
        }
    }
}
