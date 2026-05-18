package com.ovx.openvino.genai.android;

import android.content.Context;
import com.ovx.openvino.genai.OpenVinoGenAiRuntime;
import com.ovx.openvino.genai.PluginRegistration;
import com.ovx.openvino.genai.RuntimeConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AndroidOpenVinoGenAiRuntime {
    private AndroidOpenVinoGenAiRuntime() {
    }

    public static PreparedRuntime prepare(Context context) {
        return prepare(context, Options.defaultOptions());
    }

    public static PreparedRuntime prepare(Context context, Options options) {
        Objects.requireNonNull(context, "context");
        Options resolvedOptions = options == null ? Options.defaultOptions() : options;
        Context appContext = context.getApplicationContext();
        File nativeLibraryDir = nativeLibraryDir(appContext);
        File javaApiLibrary = nativeLibraryDir.toPath()
                .resolve("lib" + resolvedOptions.javaApiLibraryName() + ".so")
                .toFile();
        if (!javaApiLibrary.isFile()) {
            throw new IllegalStateException("OpenVINO GenAI Java JNI bridge is missing: "
                    + javaApiLibrary.getAbsolutePath());
        }

        File runtimeDir = new File(appContext.getFilesDir(), "openvino-runtime/" + nativeLibraryDir.getName());
        runtimeDir.mkdirs();
        validateOpenVinoLibraries(nativeLibraryDir);
        copyOpenVinoPluginLibraries(nativeLibraryDir, runtimeDir);
        if (!AndroidAssetBundle.directoryExists(appContext, resolvedOptions.runtimeAssetDir())) {
            throw new IllegalStateException(
                    "OpenVINO runtime assets are missing at assets/" + resolvedOptions.runtimeAssetDir());
        }
        AndroidAssetBundle.copyDirectory(appContext, resolvedOptions.runtimeAssetDir(), runtimeDir);
        copyPluginLibrariesToVersionDirs(runtimeDir);
        return new PreparedRuntime(runtimeDir, nativeLibraryDir, javaApiLibrary, resolvedOptions);
    }

    private static File nativeLibraryDir(Context context) {
        File nativeLibraryDir = new File(context.getApplicationInfo().nativeLibraryDir);
        if (!nativeLibraryDir.isDirectory()) {
            throw new IllegalStateException("Android native library directory is not available. "
                    + "Use legacy JNI packaging for OpenVINO GenAI.");
        }
        return nativeLibraryDir;
    }

    private static void validateOpenVinoLibraries(File nativeLibraryDir) {
        File[] files = nativeLibraryDir.listFiles(file -> file.isFile()
                && file.getName().endsWith(".so")
                && (file.getName().startsWith("libopenvino")
                || file.getName().startsWith("libtbb")
                || file.getName().equals("libc++_shared.so")));
        List<File> libraries = files == null ? List.of() : List.of(files);
        if (libraries.stream().noneMatch(file -> file.getName().equals("libopenvino.so"))) {
            throw new IllegalStateException("OpenVINO runtime library is missing in " + nativeLibraryDir);
        }
        if (libraries.stream().noneMatch(file -> file.getName().equals("libopenvino_genai.so"))) {
            throw new IllegalStateException("OpenVINO GenAI library is missing in " + nativeLibraryDir);
        }
    }

    private static void copyOpenVinoPluginLibraries(File nativeLibraryDir, File runtimeDir) {
        File[] pluginLibraries = nativeLibraryDir.listFiles(file -> file.isFile()
                && file.getName().startsWith("libopenvino_")
                && file.getName().endsWith("_plugin.so"));
        if (pluginLibraries == null) {
            return;
        }
        for (File source : pluginLibraries) {
            copyToIfChanged(source, new File(runtimeDir, source.getName()));
        }
    }

    private static void copyPluginLibrariesToVersionDirs(File runtimeDir) {
        File[] pluginLibraries = runtimeDir.listFiles(file -> file.isFile()
                && file.getName().startsWith("libopenvino_")
                && file.getName().endsWith("_plugin.so"));
        File[] pluginDirs = runtimeDir.listFiles(file -> file.isDirectory() && file.getName().startsWith("openvino-"));
        if (pluginLibraries == null || pluginDirs == null) {
            return;
        }
        for (File pluginDir : pluginDirs) {
            for (File library : pluginLibraries) {
                copyToIfChanged(library, new File(pluginDir, library.getName()));
            }
        }
    }

    private static void copyToIfChanged(File source, File target) {
        if (target.isFile() && target.length() == source.length()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try {
            java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            throw new IllegalStateException("Unable to copy " + source + " to " + target, cause);
        }
    }

    public static final class PreparedRuntime {
        private final File runtimeDir;
        private final File nativeLibraryDir;
        private final File javaApiLibrary;
        private final Options options;

        private PreparedRuntime(File runtimeDir, File nativeLibraryDir, File javaApiLibrary, Options options) {
            this.runtimeDir = runtimeDir;
            this.nativeLibraryDir = nativeLibraryDir;
            this.javaApiLibrary = javaApiLibrary;
            this.options = options;
        }

        public void initialize(String deviceName) {
            RuntimeConfiguration.Builder configuration = RuntimeConfiguration.builder();
            for (String libraryName : options.preferredLibraryLoadOrder()) {
                File library = new File(nativeLibraryDir, libraryName);
                if (library.isFile()) {
                    configuration.loadAbsoluteLibrary(library.getAbsolutePath());
                }
            }

            File pluginLibrary = resolvePluginLibrary(deviceName);
            if (pluginLibrary != null && pluginLibrary.isFile()) {
                configuration.registerPlugin(
                        PluginRegistration.builder(deviceName, pluginLibrary.getAbsolutePath()).build());
            }

            configuration.loadAbsoluteLibrary(javaApiLibrary.getAbsolutePath());
            OpenVinoGenAiRuntime.initialize(configuration.build());
        }

        public File runtimeDir() {
            return runtimeDir;
        }

        public File nativeLibraryDir() {
            return nativeLibraryDir;
        }

        private File resolvePluginLibrary(String deviceName) {
            if (options.pluginLibraryName() != null) {
                return new File(runtimeDir, options.pluginLibraryName());
            }
            String normalizedDevice = deviceName.toLowerCase(Locale.ROOT);
            File[] plugins = runtimeDir.listFiles(file -> file.isFile()
                    && file.getName().startsWith("libopenvino_")
                    && file.getName().endsWith("_plugin.so"));
            if (plugins == null) {
                return null;
            }
            for (File plugin : plugins) {
                String name = plugin.getName().toLowerCase(Locale.ROOT);
                if (name.contains("_" + normalizedDevice + "_")
                        || name.equals("libopenvino_" + normalizedDevice + "_plugin.so")) {
                    return plugin;
                }
            }
            return null;
        }
    }

    public static final class Options {
        private static final List<String> DEFAULT_LIBRARY_LOAD_ORDER = List.of(
                "libc++_shared.so",
                "libtbb.so",
                "libtbbmalloc.so",
                "libtbbmalloc_proxy.so",
                "libopenvino.so",
                "libopenvino_c.so",
                "libopenvino_ir_frontend.so",
                "libopenvino_tokenizers.so",
                "libopenvino_genai.so",
                "libopenvino_genai_c.so");

        private final String javaApiLibraryName;
        private final String runtimeAssetDir;
        private final String pluginLibraryName;
        private final List<String> preferredLibraryLoadOrder;

        private Options(Builder builder) {
            this.javaApiLibraryName = builder.javaApiLibraryName;
            this.runtimeAssetDir = builder.runtimeAssetDir;
            this.pluginLibraryName = builder.pluginLibraryName;
            this.preferredLibraryLoadOrder = List.copyOf(builder.preferredLibraryLoadOrder);
        }

        public static Options defaultOptions() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        public String javaApiLibraryName() {
            return javaApiLibraryName;
        }

        public String runtimeAssetDir() {
            return runtimeAssetDir;
        }

        public String pluginLibraryName() {
            return pluginLibraryName;
        }

        public List<String> preferredLibraryLoadOrder() {
            return preferredLibraryLoadOrder;
        }

        public static final class Builder {
            private String javaApiLibraryName = "ov_genai_java_jni";
            private String runtimeAssetDir = "openvino-runtime";
            private String pluginLibraryName;
            private List<String> preferredLibraryLoadOrder = DEFAULT_LIBRARY_LOAD_ORDER;

            private Builder() {
            }

            public Builder javaApiLibraryName(String javaApiLibraryName) {
                this.javaApiLibraryName = Objects.requireNonNull(javaApiLibraryName, "javaApiLibraryName");
                return this;
            }

            public Builder runtimeAssetDir(String runtimeAssetDir) {
                this.runtimeAssetDir = Objects.requireNonNull(runtimeAssetDir, "runtimeAssetDir");
                return this;
            }

            public Builder pluginLibraryName(String pluginLibraryName) {
                this.pluginLibraryName = Objects.requireNonNull(pluginLibraryName, "pluginLibraryName");
                return this;
            }

            public Builder preferredLibraryLoadOrder(List<String> preferredLibraryLoadOrder) {
                this.preferredLibraryLoadOrder = List.copyOf(
                        Objects.requireNonNull(preferredLibraryLoadOrder, "preferredLibraryLoadOrder"));
                return this;
            }

            public Options build() {
                return new Options(this);
            }
        }
    }
}
