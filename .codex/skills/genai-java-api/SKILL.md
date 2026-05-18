---
name: genai-java-api
description: Work on the `genai-java-api` repository, especially public docs, repo-local skills, Android GFX packaging, JNI bridge mode selection, Java API updates under `com.ovx.openvino.genai`, and publication hygiene that keeps local technical data out of commits.
---

# genai-java-api

## Overview

Use this skill when updating the OpenVINO GenAI Java/JNI wrapper repo. Treat the live code as the authority, and keep public documentation aligned with the Java API, native JNI bridge modes, and Android GFX deployment path.

## Publication Boundary

- Do not edit or stage `AGENTS.md` unless the user explicitly asks for it.
- Keep `.tmp/`, `.internal-docs/`, device logs, local model files, generated bundles, Gradle caches, and native build trees out of commits.
- Remove private machine paths, personal usernames, private architecture-note links, and local bug/debug logs from public docs and helper defaults.
- Use environment variables and `/path/to/...` placeholders in public scripts and documentation.
- Keep Android's active convenience path GFX-first: `DeviceSelection.gfx()` and `PluginRegistration.gfx(...)`.

## Update Workflow

1. Check `git status --short --branch` before editing so unrelated local changes stay visible.
2. Inspect changed Java and native files before updating docs; do not infer behavior from older notes.
3. Refresh `README.md` and files under `docs/` whenever the public Java surface, JNI mode matrix, tool variables, or Android packaging flow changes.
4. Refresh this skill when future agents need new non-obvious repo rules or validation steps. Keep it short; do not add auxiliary README-style files inside the skill.
5. Run publication hygiene checks before commit:

```bash
git diff --check
rg -n "(?:anes)terov|/(?:Users)|[.](?:tmp/local_docs)|deep-(?:research)" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
rg -n "(?:sec)ret|(?:pass)word|api[_-]?key|access[_-]?token" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
```

6. Run Java tests with `gradle test` when Gradle is available. Native Android validation requires external OpenVINO/OpenVINO GenAI Android artifacts and should be reported separately if those artifacts are unavailable.
7. For `tools/stage_android_runtime_assets.py`, use a package-shaped tree under ignored `build/` for local smoke checks; never stage real runtime assets into tracked directories.

## Repo Facts

- Public Java package: `com.ovx.openvino.genai`.
- Native internal package: `com.ovx.openvino.genai.internal`.
- JNI build modes: `AUTO`, `REAL`, and `STUB` through `OV_GENAI_JNI_MODE`.
- Stub JNI is for API compilation and tests only; it is not an inference implementation.
- `PipelineProperties` carries per-pipeline OpenVINO/OpenVINO GenAI `ov::AnyMap` options; `RuntimeConfiguration` is only for native library loading and plugin registration.
- `GenerationPerfMetrics` is a Java convenience view over `GenerationResult.perfMetrics()`; keep its documented keys aligned with `perf_metrics_to_java_map` in `ov_genai_java_jni_real.cpp`.
- `AndroidPipelineProperties.cpuLatency(...)` is an explicit CPU preset; do not present it as the default Android path.
- Android framework-dependent helpers live in `src/android/java`; desktop Gradle tests do not compile that source tree unless the build is extended with Android SDK inputs.
- Android helper scripts live in `tools/` and should require caller-provided OpenVINO, OpenVINO GenAI, Android NDK, model, package, and device inputs rather than hardcoded local paths.
