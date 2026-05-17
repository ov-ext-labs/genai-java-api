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
4. Refresh this skill when future agents need new non-obvious repo rules or validation steps.
5. Run publication hygiene checks before commit:

```bash
git diff --check
rg -n "[a]nesterov|[/]Users|[.]tmp/local_docs|deep[-]research" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
rg -n "[s]ecret|[p]assword|api[_-]?key|access[_-]?token" \
  --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!.tmp/**' -g '!.internal-docs/**'
```

6. Run Java tests with `gradle test` when Gradle is available. Native Android validation requires external OpenVINO/OpenVINO GenAI Android artifacts and should be reported separately if those artifacts are unavailable.

## Repo Facts

- Public Java package: `com.ovx.openvino.genai`.
- Native internal package: `com.ovx.openvino.genai.internal`.
- JNI build modes: `AUTO`, `REAL`, and `STUB` through `OV_GENAI_JNI_MODE`.
- Stub JNI is for API compilation and tests only; it is not an inference implementation.
- Android helper scripts live in `tools/` and should require caller-provided OpenVINO, OpenVINO GenAI, Android NDK, model, and device inputs rather than hardcoded local paths.
