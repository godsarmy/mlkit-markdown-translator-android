# ML Kit Markdown Translator

Reusable Android library for translating Markdown content with Google ML Kit while preserving Markdown structure (code blocks, links, headings, lists, and spacing) as much as possible.

## Project status

This repository is being built incrementally from the TODO roadmap in `ML-markdown-lib.TODO.md`.

## Planned modules

- `library/` — Android library module (core deliverable)
- `sample/` — Android sample app for manual verification
- `docs/` — design notes and API documentation

## API draft

See `docs/api.md` for the initial public API definition.

## Using from Android app

### 1) Gradle setup

Current recommended adoption is **local module / included build** while API stabilizes.

If this repo is included in your project, add:

```gradle
dependencies {
    implementation project(":library")
}
```

### 2) Basic usage in app layer

```java
MlKitMarkdownTranslator translator = new MlKitMarkdownTranslator();

translator.ensureLanguageModelDownloaded("es", new OperationCallback() {
    @Override
    public void onSuccess() {
        translator.translateMarkdown(markdown, "en", "es", new TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                // update UI / state with translated markdown
            }

            @Override
            public void onFailure(Exception error) {
                // handle translation failure
            }
        });
    }

    @Override
    public void onFailure(Exception error) {
        // handle model download failure
    }
});
```

### Java quickstart (copy/paste)

```java
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

public final class MarkdownTranslationController {
    private final MlKitMarkdownTranslator translator = new MlKitMarkdownTranslator();

    public void translate(String markdown) {
        translator.ensureLanguageModelDownloaded("es", new OperationCallback() {
            @Override
            public void onSuccess() {
                translator.translateMarkdown(markdown, "en", "es", new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // update your Java UI layer
                    }

                    @Override
                    public void onFailure(Exception error) {
                        // show translation error state
                    }
                });
            }

            @Override
            public void onFailure(Exception error) {
                // show model-download error state
            }
        });
    }

    public void close() {
        translator.close();
    }
}
```

Lifecycle reminder for Java Activities/Fragments:

- create one translator instance per screen/controller scope
- call `close()` from `onDestroy()` (or equivalent owner teardown)
- avoid creating a new translator per button click

Common failure handling recommendations:

- model download failure: show retry action and keep source markdown intact
- translation failure: preserve original markdown + show error UI state
- unsupported language code: validate language selection before triggering translation

Additional Java repository-style example:

- `docs/examples/JavaMarkdownTranslationRepositoryExample.java`

### 3) ViewModel/Repository split (recommended)

- **Activity/Fragment**: owns UI state and triggers user actions.
- **ViewModel**: coordinates language selection + calls into repository.
- **Repository**: wraps `MlKitMarkdownTranslator` calls (`ensureLanguageModelDownloaded`,
  `translateMarkdown`, `getDownloadedLanguagePacks`, `deleteLanguagePack`).

This keeps app UI logic in app code while central markdown-translation logic stays in the library.

### 4) Migration notes for `android-nixpkgs`

Replace app-local helper flow with this library:

- replace current `MarkdownTranslationHelper` orchestration with `MlKitMarkdownTranslator`
- remove duplicated ML Kit model-management code where possible and use:
  - `ensureLanguageModelDownloaded(...)`
  - `getDownloadedLanguagePacks(...)`
  - `deleteLanguagePack(...)`

Target migration shape:

- keep screen/state/UI logic in `android-nixpkgs`
- move markdown translation and model concerns to this library API

### 5) Integration guardrails

- **Android SDK**: current library module targets `minSdk 24`, `compileSdk 34`.
- **Permissions**: host app should include network permission when model downloads are expected:

  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```

- **Threading**: callbacks can arrive asynchronously; marshal UI updates to main thread in Java apps.
- **Model lifecycle**:
  - call `ensureLanguageModelDownloaded(...)` before first translation for a target language
  - use `getDownloadedLanguagePacks(...)` and `deleteLanguagePack(...)` for storage control
- **Resource lifecycle**: call `translator.close()` from owner teardown (`onDestroy()` or equivalent).
- **R8/ProGuard**: no custom keep rules are currently required for the public API surface; re-check if
  reflection-based integrations are added later.

## Distribution strategy

Current recommendation follows the roadmap:

1. **Local included build** (now)
   - fastest iteration while API and behavior are still stabilizing
   - easiest debugging across app + library changes
2. **Git submodule** (optional intermediate)
   - useful when you want explicit repo linkage without publishing
3. **JitPack** (next external distribution step)
   - practical for simple dependency consumption from Android projects
4. **Maven Central** (later)
   - only after API maturity, stricter compatibility expectations, and release process hardening

In short: start with local/submodule, then move to JitPack once the API is stable enough for broader reuse.

### JitPack usage (when tags are published)

```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.godsarmy:mlkit-markdown-translator-android:<tag>"
}
```

Project includes `jitpack.yml` for a library-focused JitPack build path.

## Limitations (v1)

This library is designed to preserve Markdown structure during translation, but v1 still has known limits:

- advanced nested Markdown edge cases may not be perfectly preserved
- raw HTML blocks beyond currently supported patterns may not round-trip cleanly
- GFM-style pipe tables are supported in AST path; regex fallback may preserve full table blocks without translating each cell
- reference-style link definitions may be imperfect if tokenization does not fully cover a case
- translator-driven punctuation drift can still occur in plain text regions

For production use, validate your own Markdown fixtures with the provided golden-test pattern under
`library/src/test/resources/fixtures`.

## License

Apache License 2.0. See `LICENSE`.
