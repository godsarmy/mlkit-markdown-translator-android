# ML Markdown Translation Library TODO

## Goal

Create a new reusable **Android Java library project** that uses **Google ML Kit Translation** to translate Markdown content while preserving the original Markdown structure as much as possible.

This library should be usable from `android-nixpkgs` and potentially other Android projects.

---

## Recommended project type

- Use a **new Git repository**.
- Use a **Gradle Android project**.
- Main deliverable should be an **Android library module**.
- Keep as much logic as possible in **plain Java** classes so it can be unit tested easily.

Reason:

- ML Kit translation APIs are Android-specific.
- A plain Java-only project would not integrate ML Kit directly without extra abstraction/workarounds.
- An Android library project still allows pure Java core logic and Android integration where needed.

---

## Suggested repository name

Choose one of:

- `ml-markdown-translator`
- `mlkit-markdown-translator`
- `android-ml-markdown-lib`

---

## Expected output of the library

Input:

- markdown string
- source language code
- target language code

Output:

- translated markdown string

Behavior goals:

- preserve fenced code blocks
- preserve inline code
- preserve markdown links and images
- preserve autolinks
- preserve paragraph/newline structure
- preserve headings/list prefixes/blockquote markers
- avoid translating code-like or token-like content
- keep output renderable by markdown engines such as Markwon

---

## Step 1 - Create repository

### Tasks

- Initialize a new empty Git repository.
- Create a Gradle project.
- Add `.gitignore` for Android/Gradle/IDE files.
- Add `README.md`.
- Add `LICENSE`.

### Suggested top-level structure

```text
ml-markdown-translator/
  README.md
  LICENSE
  settings.gradle
  build.gradle
  gradle.properties
  gradlew
  gradlew.bat
  library/
  sample/
  docs/
```

### Notes

- `library/` = Android library module.
- `sample/` = optional sample app for manual testing.
- `docs/` = design notes, roadmap, API notes.

---

## Step 2 - Create Android library module

### Tasks

- Create module `library` as `com.android.library`.
- Use Java as the main implementation language.
- Set sensible minSdk/targetSdk values.
- Add AndroidX annotations dependency if useful.

### Deliverables

- `library/build.gradle`
- `library/src/main/AndroidManifest.xml`
- initial package namespace, for example:
  - `io.github.<you>.mlmarkdown`

### Notes

- Keep public API small.
- Avoid app-specific UI or storage logic in this module.

---

## Step 3 - Add sample app module

### Tasks

- Create `sample/` Android app module.
- Depend on `:library`.
- Build a small UI for manual testing.

### Sample app features

- text area for source markdown
- source language selector
- target language selector
- button to download model
- button to translate
- preview area for original markdown
- preview area for translated markdown
- optional rendered markdown preview using Markwon

### Why this matters

- You will need fast feedback for broken markdown structure.
- Manual QA is much easier in a sample app than inside production app code.

---

## Step 4 - Define public API before implementation

### Tasks

- Write down the public API in `README.md` or `docs/api.md` first.
- Keep API stable and minimal.

### Recommended API shape

#### Translation engine abstraction

```java
public interface TranslationEngine {
  void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback);
}
```

#### Markdown translator facade

```java
public interface MarkdownTranslator {
  void translateMarkdown(
      String markdown,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);
}
```

#### Model management facade

```java
public interface LanguageModelManager {
  void ensureModelDownloaded(String targetLanguage, OperationCallback callback);
  void getDownloadedModels(LanguagePacksCallback callback);
  void deleteModel(String languageCode, OperationCallback callback);
}
```

### Recommended concrete classes

- `MlKitTranslationEngine`
- `DefaultMarkdownTranslator`
- `MlKitLanguageModelManager`

---

## Step 5 - Separate pure Java core from Android/ML Kit integration

### Goal

Keep markdown-preservation logic independent from ML Kit.

### Tasks

- Put pure text/markdown logic in plain Java classes.
- Put ML Kit calls in Android-aware classes only.

### Suggested package structure

```text
library/src/main/java/.../
  api/
    MarkdownTranslator.java
    TranslationCallback.java
    LanguagePacksCallback.java
    OperationCallback.java
  engine/
    TranslationEngine.java
    MlKitTranslationEngine.java
  markdown/
    MarkdownPreprocessor.java
    MarkdownProtectionPipeline.java
    MarkdownStructureTranslator.java
    MarkdownRestorer.java
    MarkdownToken.java
    MarkdownTokenStore.java
  model/
    ProtectedSegment.java
    TextChunk.java
  manager/
    MlKitLanguageModelManager.java
```

---

## Step 6 - Implement the markdown preprocessing pipeline

### Goal

Normalize markdown before translation and protect sections that must not be translated.

### Tasks

Implement preprocessing in this order:

1. normalize line endings
   - convert `\r\n` and `\r` to `\n`

2. normalize custom code-like tags if needed
   - convert `<block>...</block>` to fenced code block format

3. protect non-translatable markdown constructs

### Protect at minimum

- fenced code blocks
- inline code
- markdown inline links `[label](url)`
- markdown images `![alt](url)`
- reference links `[label][ref]`
- autolinks `<https://...>`
- raw URLs if needed
- HTML code-like blocks if needed

### Recommended approach

Use **token replacement**, not only regex splitting.

Example:

- original markdown fragment: `[home page](https://example.com)`
- replace with token: `@@MD_TOKEN_12@@`
- translate surrounding text
- restore token afterward

### Why tokenization is better

- easier to reason about
- easier to test
- less fragile than large chained regexes
- easier to support new markdown constructs later

---

## Step 7 - Implement structure-preserving translation

### Goal

Translate human-readable text while keeping markdown layout stable.

### Tasks

Process translatable content with explicit structure awareness.

#### Preserve newline structure

- split by newline groups
- preserve delimiters exactly (`\n`, `\n\n`, etc.)
- only translate text chunks between delimiters
- reassemble with original delimiter sequence untouched

#### Preserve markdown prefixes

For each line, detect and preserve prefixes such as:

- headings: `#`, `##`, etc.
- list bullets: `-`, `*`, `+`
- ordered lists: `1.`, `2.`
- blockquotes: `>`
- nested combinations like `> - ` or `  - `

Translate only the content after the prefix.

Example:

```text
- Install package
```

becomes internally:

- protected prefix: `- `
- translatable body: `Install package`

then restored as:

```text
- <translated body>
```

#### Preserve empty lines exactly

- do not collapse blank lines
- do not merge paragraphs
- do not trim trailing structure unless explicitly safe

---

## Step 8 - Implement translation chunking strategy

### Goal

Avoid sending huge markdown blobs to ML Kit in a single translation call.

### Tasks

- split translatable content into manageable chunks
- keep chunk boundaries aligned with markdown structure where possible
- avoid splitting inside sentences when possible

### Recommended rules

- prefer paragraph-level chunks
- fallback to line-level chunks when structure is complex
- keep chunk length under a safe threshold

### Why

- better reliability
- easier debugging
- less formatting drift

---

## Step 9 - Implement ML Kit translation adapter

### Tasks

- Add ML Kit translation dependency.
- Implement `MlKitTranslationEngine`.
- Implement language/model management wrapper.

### Features to support

- create translator for source/target language
- translate plain text chunk
- ensure model downloaded
- check model downloaded state
- list downloaded models
- delete model
- close translators cleanly

### Notes

- cache translator instances when appropriate
- make lifecycle explicit
- define callback interfaces clearly

---

## Step 10 - Build a high-level facade API

### Goal

Consumer app should use one simple class.

### Suggested facade

```java
public final class MlKitMarkdownTranslator {
  void translateMarkdown(...)
  void ensureLanguageModelDownloaded(...)
  void getDownloadedLanguagePacks(...)
  void deleteLanguagePack(...)
}
```

### Tasks

- hide internal pipeline classes from consumers
- expose only necessary callbacks and config

---

## Step 11 - Add configuration options

### Suggested optional config object

```java
public final class MarkdownTranslationOptions {
  boolean preserveNewlines;
  boolean preserveListPrefixes;
  boolean preserveBlockquotes;
  boolean normalizeCustomBlockTags;
  boolean protectAutolinks;
}
```

### Why

- makes behavior adjustable without rewriting core logic
- allows stricter modes later

---

## Step 12 - Write unit tests for pure Java logic

### Goal

This is one of the main reasons to extract the library.

### Add tests for

- line ending normalization
- `<block>` to fenced code normalization
- fenced code preservation
- inline code preservation
- inline link preservation
- image preservation
- autolink preservation
- list prefix preservation
- heading prefix preservation
- blockquote prefix preservation
- blank line preservation
- mixed markdown content preservation
- token restore correctness

### Golden test idea

Use fixture-based tests:

- `input.md`
- protected intermediate representation if useful
- expected output after mock translation

### Important

Mock the translation engine in pure Java tests.

Example mock behavior:

- return `[[TRANSLATED:<input>]]`

This makes formatting breakage easy to detect.

---

## Step 13 - Add Android integration tests

### Tasks

- Add minimal Android tests for ML Kit wrapper behavior.
- Verify callbacks and model management flow.

### Optional coverage

- model download success callback
- model delete callback
- model list retrieval

### Note

Do not over-invest here initially; most complexity is in pure Java markdown logic.

---

## Step 14 - Add sample markdown rendering verification

### Tasks

- In sample app, render translated markdown using Markwon.
- Prepare demo markdown with:
  - headings
  - lists
  - nested lists
  - blockquotes
  - links
  - images
  - fenced code blocks
  - inline code

### Goal

Visually verify that translated markdown still renders correctly.

---

## Step 15 - Define limitations clearly

### In README, document that v1 may not perfectly preserve

- advanced nested markdown edge cases
- raw HTML blocks beyond supported patterns
- tables if not explicitly handled
- reference definitions if not yet tokenized well
- translator-driven punctuation drift in plain text regions

### Why

Set correct expectations early.

---

## Step 16 - Document integration into android-nixpkgs

### Tasks

- Add section in README: "Using from Android app"
- show sample Gradle dependency setup
- show example usage from activity/viewmodel/repository

### For `android-nixpkgs`

Replace app-local helper flow with library calls:

- current `MarkdownTranslationHelper`
- current ML Kit model-management duplication if desired

### Example migration target

- keep app UI logic in app
- move markdown translation logic into library

---

## Step 17 - Decide distribution strategy

### Option A - local included build first

Best for fast development.

### Option B - Git submodule

Good if you want repository linkage but not publishing yet.

### Option C - JitPack

Good for easy dependency use from Android projects.

### Option D - Maven Central

Only when API is stable and project is mature.

### Recommendation

Start with **local included build or submodule**, then move to **JitPack** later.

---

## Step 18 - Suggested milestone plan

### Milestone 1 - bootstrap

- create repo
- create Android library module
- create sample app
- define public API

### Milestone 2 - core preservation

- implement token protection
- implement newline preservation
- implement markdown prefix preservation
- add unit tests

### Milestone 3 - ML Kit integration

- implement ML Kit engine
- implement model management API
- wire sample app

### Milestone 4 - stabilization

- add golden tests
- add README/docs
- integrate into `android-nixpkgs`

### Milestone 5 - publishing

- version tags
- changelog
- JitPack or Maven publication

---

## Step 19 - First concrete implementation checklist

Use this checklist to bootstrap the repo in order.

### Bootstrap checklist

- [ ] Create Git repo
- [ ] Initialize Gradle Android project
- [ ] Add `library` module
- [ ] Add `sample` module
- [ ] Add ML Kit dependency to `library`
- [ ] Add `README.md`
- [ ] Add package namespace
- [ ] Add public callback interfaces
- [ ] Add `TranslationEngine` interface
- [ ] Add `MlKitTranslationEngine`
- [ ] Add `MlKitLanguageModelManager`
- [ ] Add `DefaultMarkdownTranslator`
- [ ] Add tokenization/protection pipeline
- [ ] Add newline-preserving translation pipeline
- [ ] Add markdown prefix-preservation logic
- [ ] Add pure Java unit tests
- [ ] Add sample UI for manual translation testing
- [ ] Add rendered markdown preview in sample app
- [ ] Add integration examples to README
- [ ] Integrate library into `android-nixpkgs`

---

## Step 20 - Suggested initial class skeletons

### `TranslationCallback.java`

```java
public interface TranslationCallback {
  void onSuccess(String translatedText);
  void onFailure(Exception error);
}
```

### `OperationCallback.java`

```java
public interface OperationCallback {
  void onSuccess();
  void onFailure(Exception error);
}
```

### `LanguagePacksCallback.java`

```java
public interface LanguagePacksCallback {
  void onSuccess(List<String> languageCodes);
  void onFailure(Exception error);
}
```

### `MarkdownTranslator.java`

```java
public interface MarkdownTranslator {
  void translateMarkdown(
      String markdown,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);
}
```

### `TranslationEngine.java`

```java
public interface TranslationEngine {
  void translate(
      String text,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);
}
```

---

## Step 21 - Success criteria

The library is ready to integrate when all are true:

- translated markdown still renders properly in sample app
- code blocks remain untouched
- links do not degrade into raw `[]()` text unexpectedly
- blank lines and paragraphs stay stable
- lists/headings/quotes still render correctly
- downloaded model management works
- unit tests cover main markdown preservation rules

---

## Step 22 - Nice-to-have future improvements

- markdown AST-based parser instead of regex/token hybrid
- support CommonMark edge cases more strictly
- configurable preserve rules
- optional caching layer
- optional sentence segmentation heuristics
- multi-engine support beyond ML Kit
- Kotlin wrapper API

---

## Final recommendation

For your use case, this is worthwhile **if** you want:

- reuse across projects
- a testable, isolated markdown translation layer
- cleaner separation from `android-nixpkgs`

If you start this repo, begin with:

1. Android library module
2. pure Java markdown-preservation pipeline
3. ML Kit adapter
4. sample app
5. tests

That path gives the best balance of reuse, quality, and maintainability.
