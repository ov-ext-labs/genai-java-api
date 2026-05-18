package com.ovx.openvino.genai.android;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public final class AndroidAssetBundle {
    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private AndroidAssetBundle() {
    }

    public static boolean directoryExists(Context context, String assetPath) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(assetPath, "assetPath");
        try {
            String[] children = context.getAssets().list(assetPath);
            return children != null && children.length > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static String readTextOrNull(Context context, String assetPath) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(assetPath, "assetPath");
        try (InputStream input = context.getAssets().open(assetPath)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static void copyDirectory(Context context, String assetPath, File targetDir) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(assetPath, "assetPath");
        Objects.requireNonNull(targetDir, "targetDir");
        String[] children;
        try {
            children = context.getAssets().list(assetPath);
        } catch (IOException cause) {
            throw new IllegalStateException("Unable to list Android asset directory: " + assetPath, cause);
        }
        if (children == null) {
            throw new IllegalStateException("Unable to list Android asset directory: " + assetPath);
        }
        for (String child : children) {
            copyAssetChild(context, assetPath, targetDir, child);
        }
    }

    private static void copyAssetChild(Context context, String assetPath, File targetDir, String child) {
        String childAssetPath = assetPath + "/" + child;
        File childTarget = new File(targetDir, child);
        String[] nestedChildren;
        try {
            nestedChildren = context.getAssets().list(childAssetPath);
        } catch (IOException cause) {
            throw new IllegalStateException("Unable to list Android asset: " + childAssetPath, cause);
        }
        if (nestedChildren == null || nestedChildren.length == 0) {
            copyAssetFile(context, childAssetPath, childTarget);
        } else {
            childTarget.mkdirs();
            copyDirectory(context, childAssetPath, childTarget);
        }
    }

    private static void copyAssetFile(Context context, String assetPath, File targetFile) {
        File parent = targetFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        try (InputStream input = context.getAssets().open(assetPath);
                OutputStream output = Files.newOutputStream(targetFile.toPath())) {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException cause) {
            throw new IllegalStateException("Unable to copy Android asset: " + assetPath, cause);
        }
    }
}
