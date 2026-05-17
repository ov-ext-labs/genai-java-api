package com.ovx.openvino.genai.internal;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

public final class JsonSupport {
    private JsonSupport() {
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = mapValue.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                builder.append(toJson(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJson(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                builder.append(toJson(iterator.next()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(toJson(Array.get(value, i)));
            }
            builder.append(']');
            return builder.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    public static String escape(String input) {
        StringBuilder builder = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }
}
