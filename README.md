# AI Video Editing Studio

This Android application is a safety-first foundation for an AI-assisted video editing workflow. It supports English and Bangla instructions in the dashboard, secure system media selection, local media metadata inspection, validated version-1 editing plans, approval-gated task states, Android Keystore-backed provider-key storage, and guarded CapCut launch detection.

## Implemented

- Dashboard with instruction input, media picker, plan preview, task status, provider settings, accessibility settings, and CapCut launch.
- Optional OpenAI and Gemini plan requests over HTTPS. API keys are entered in the app and encrypted with an Android Keystore AES-GCM key. No key is stored in source, resources, logs, or CI.
- Strict JSON plan schema and allowlisted actions. Plans containing unknown actions, arbitrary command fields, or unconfirmed exports are rejected.
- Local metadata inspection through `MediaMetadataRetriever`; selected media is not uploaded by the local analysis flow.
- Explicit task states: `PENDING`, `VALIDATING`, `RUNNING`, `WAITING_FOR_USER`, `VERIFYING`, `COMPLETED`, `FAILED`, and `CANCELLED`.
- Accessibility event text and package-name logging are intentionally disabled.
- GitHub Actions runs unit tests and builds the debug APK using the committed Gradle wrapper, JDK 17, Android Platform 34, and Build Tools 34.0.0.

## Deliberate limitations

The offline planner is not presented as AI: it creates an analysis-only draft. Provider-backed plans require a user-supplied API key and still require approval. The current executor supports only local metadata analysis; trim, merge, subtitles, audio mixing, export, and other editing actions fail explicitly as unsupported rather than reporting false success.

CapCut automation currently detects and launches an installed CapCut package. It does not claim reliable draft navigation, timeline editing, export, or UI automation because those behaviors require testing against the installed CapCut version on a real device. No CapCut account, paid feature, security control, or access restriction is bypassed.

## Build

```bash
./gradlew testDebugUnitTest
./gradlew clean assembleDebug --stacktrace
```

The CI workflow is `.github/workflows/build-android.yml`. The application ID is `com.example.myautomationapp`, version `1.0` / version code `1`, with minimum SDK 24 and target SDK 34.
