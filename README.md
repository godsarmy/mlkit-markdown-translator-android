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

## Limitations (v1)

This library is designed to preserve Markdown structure during translation, but v1 still has known limits:

- advanced nested Markdown edge cases may not be perfectly preserved
- raw HTML blocks beyond currently supported patterns may not round-trip cleanly
- tables are not guaranteed unless explicitly handled by the active pipeline path
- reference-style link definitions may be imperfect if tokenization does not fully cover a case
- translator-driven punctuation drift can still occur in plain text regions

For production use, validate your own Markdown fixtures with the provided golden-test pattern under
`library/src/test/resources/fixtures`.

## License

Apache License 2.0. See `LICENSE`.
