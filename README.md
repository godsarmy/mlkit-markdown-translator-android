# ML Kit Markdown Translator

[![JitPack](https://jitpack.io/v/godsarmy/mlkit-markdown-translator-android.svg)](https://jitpack.io/#godsarmy/mlkit-markdown-translator-android)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/android-minSdk%2024-brightgreen.svg)](library/build.gradle)

<img src="docs/icon.svg" alt="ML Kit Markdown Translator icon" width="96" />

Translate Markdown in Android apps with Google ML Kit while keeping the document readable as Markdown.

This library prepares Markdown for translation, sends only the translatable text through ML Kit, and rebuilds the document with headings, lists, quotes, links, code, tables, and spacing preserved as much as possible. It is designed for apps that need local, ML Kit-powered translation without flattening rich Markdown content into plain text.

## Why use it?

- **Markdown-aware translation** — protects structural syntax while translating human-readable text.
- **Built on Google ML Kit Translate** — uses on-device translation models managed by your app.
- **Small Java API** — simple callback-based integration for Android projects.
- **AST/token-based pipeline** — avoids relying on regex-only Markdown handling.
- **Diagnostics included** — inspect chunking and tokenization with `explainMarkdown(...)`.
- **Sample app included** — see a practical integration with model management and rendered preview.

Requirements: Android `minSdk 24`, Java 17, and an app-level ML Kit model download flow.

## Example app

<img src="screenshot-example.jpg" alt="Screenshot of the example app" width="360" />

The `sample/` app shows how to build a complete Markdown translation experience around the library. It demonstrates:

- Markdown input and translated output
- source and target language selection
- ML Kit model download and delete flow
- raw Markdown and rendered Markdown preview modes
- missing-model handling and user-facing error states

Build it from the repository root:

```bash
./gradlew :sample:assembleDebug
```

Generated APK:

```text
sample/build/outputs/apk/debug/sample-debug.apk
```

Install to a connected device:

```bash
adb install -r sample/build/outputs/apk/debug/sample-debug.apk
```

Google Play listing:

```text
https://play.google.com/store/apps/details?id=io.github.godsarmy.mlmarkdown.sample
```

## Installation

### JitPack

JitPack page: https://jitpack.io/#godsarmy/mlkit-markdown-translator-android

Groovy:

```gradle
repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.godsarmy:mlkit-markdown-translator-android:1.2.0"
}
```

Kotlin DSL:

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.godsarmy:mlkit-markdown-translator-android:1.2.0")
}
```

### Local module

If you are working with this repository as part of a multi-module build:

```gradle
dependencies {
    implementation project(":library")
}
```

## Quick start

```java
MlKitMarkdownTranslator translator = new MlKitMarkdownTranslator();

translator.translateMarkdown(markdown, "en", "es", new TranslationCallback() {
    @Override
    public void onSuccess(String translatedMarkdown) {
        // Render or store the translated Markdown.
    }

    @Override
    public void onFailure(Exception error) {
        // Show an error state or request a missing language model.
    }
});
```

Create one translator per screen/controller scope and call `close()` when that scope is destroyed.

## Handling ML Kit models

`translateMarkdown(...)` does not automatically download missing language models. Manage model lifecycle in your app with ML Kit APIs such as:

- `RemoteModelManager`
- `TranslateRemoteModel`
- `DownloadConditions`

Handle missing models explicitly:

```java
import io.github.godsarmy.mlmarkdown.api.TranslationErrorCode;
import io.github.godsarmy.mlmarkdown.api.TranslationException;

if (error instanceof TranslationException
        && ((TranslationException) error).getCode() == TranslationErrorCode.MODEL_NOT_DOWNLOADED) {
    // Ask the user to download the required ML Kit language model.
}
```

If your app downloads models, include internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Inspect Markdown processing

Use `explainMarkdown(...)` to understand how the library chunks and protects Markdown before translation. This is useful when testing your own Markdown fixtures or debugging edge cases.

```java
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownChunk;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;

MlKitMarkdownTranslator translator = new MlKitMarkdownTranslator();
ExplainMarkdownResult explain = translator.explainMarkdown(markdown);

for (ExplainMarkdownChunk chunk : explain.getChunks()) {
    Log.d("MLMD", "chunk #" + chunk.getIndex() + " raw=" + chunk.getRawText());
}

Log.d("MLMD", "mode=" + explain.getProcessingMode()
        + " tokens=" + explain.getTotalTokenCount()
        + " chunks=" + explain.getTotalChunkCount());
```

`explainMarkdown(...)` runs local preparation diagnostics only; it does not call ML Kit translation.

## Version notes

The library currently defaults to:

- `com.google.mlkit:translate:17.0.3`
- `com.vladsch.flexmark:flexmark:0.64.8`

When integrating through JitPack, you can pin a newer ML Kit version in your app if needed:

```gradle
dependencies {
    implementation "com.github.godsarmy:mlkit-markdown-translator-android:1.2.0"
    implementation "com.google.mlkit:translate:17.0.4"
}
```

For local-module development, override versions in root `gradle.properties`:

```properties
mlkitTranslateVersion=17.0.4
flexmarkVersion=0.64.8
```

## Documentation

- Public API reference: [`docs/api.md`](docs/api.md)
- Architecture and pipeline notes: [`docs/architecture.md`](docs/architecture.md)
- Repository-style integration example: [`docs/examples/JavaMarkdownTranslationRepositoryExample.java`](docs/examples/JavaMarkdownTranslationRepositoryExample.java)

Repository layout:

- `library/` — reusable Android library module
- `sample/` — example Android app
- `docs/` — API, architecture, and integration notes

## Markdown compatibility notes

The library is designed to preserve Markdown structure during translation, but translation engines can still change punctuation or spacing in plain-text regions. Validate your own Markdown corpus if your app depends on strict round-tripping.

Known practical limits:

- very complex nested Markdown may need app-specific verification
- raw HTML blocks are preserved only for supported patterns
- GFM pipe tables are translated through the AST path; fallback mode may preserve full table blocks without translating every cell
- unusual reference-style links may need fixture coverage

Golden-test fixtures live under `library/src/test/resources/fixtures`.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
