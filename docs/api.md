# Public API Reference

This file documents the current public API exposed by the library.

## Main facade

### `MlKitMarkdownTranslator`

Package: `io.github.godsarmy.mlmarkdown`

```java
public final class MlKitMarkdownTranslator implements Closeable {
  public MlKitMarkdownTranslator();
  public MlKitMarkdownTranslator(MarkdownTranslationOptions options);

  public void translateMarkdown(
      String markdown,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);

  public void translateMarkdown(
      String markdown,
      String sourceLanguage,
      String targetLanguage,
      long timeoutMs,
      TranslationCallback callback);

  public ExplainMarkdownResult explainMarkdown(String markdown);

  @Override
  public void close();
}
```

### Usage notes

- Manage ML Kit language models in app code via native ML Kit APIs (`RemoteModelManager`,
  `TranslateRemoteModel`).
- `translateMarkdown(...)` does not auto-download missing models; translation fails when the
  required pack is unavailable.
- `translateMarkdown(..., timeoutMs, ...)`
  - `timeoutMs = 0` (default): no timeout.
  - `timeoutMs > 0`: fails with `TranslationException` (`TranslationErrorCode.TIMEOUT`) if the
    active translation engine call does not complete before timeout.
  - In Markdown mode, translation is chunked; timeout is evaluated per chunk/token call, not as a
    single wall-clock timeout for the full document.
- `explainMarkdown(...)` is a fast, local preparation/chunking diagnostic path and does not call
  the translation engine.
- Reuse one `MlKitMarkdownTranslator` instance per screen/controller scope.
- Call `close()` when owner is destroyed.

## Explain/diagnostics API

### `ExplainMarkdownResult`

```java
public final class ExplainMarkdownResult {
  public ProcessingMode getProcessingMode();
  public String getPreparedMarkdown();
  public List<ExplainMarkdownToken> getTokens();
  public List<ExplainMarkdownChunk> getChunks();
  public List<ExplainProtectedSegment> getProtectedSegments();
  public int getTotalTokenCount();
  public int getTotalChunkCount();
}
```

### `ExplainMarkdownToken`

```java
public final class ExplainMarkdownToken {
  public MarkdownTokenType getType();
  public @Nullable String getTokenId();
  public String getValue();
  public int getStartOffset();
  public int getEndOffset();
}
```

### `ExplainMarkdownChunk`

```java
public final class ExplainMarkdownChunk {
  public int getIndex();
  public String getRawText();
  public List<String> getTokenIds();
  public List<String> getTokenValues();
  public int getPlainTextLength();
}
```

### `ExplainProtectedSegment`

```java
public final class ExplainProtectedSegment {
  public String getToken();
  public String getOriginalText();
}
```

## Configuration

### `MarkdownTranslationOptions`

Package: `io.github.godsarmy.mlmarkdown`

```java
public final class MarkdownTranslationOptions {
  public static MarkdownTranslationOptions defaults();

  public boolean preserveNewlines();
  public boolean preserveListPrefixes();
  public boolean preserveBlockquotes();
  public boolean normalizeCustomBlockTags();
  public boolean protectAutolinks();
  public boolean enableRegexFallbackProtection();
  public boolean preserveWhitespaceAroundProtectedSegments();
  public String escapedMarkdownCharactersToProtect();
  public String tokenMarker();
  public int maxCharsPerChunk();
  public @Nullable TranslationTimingListener translationTimingListener();

  public static final class Builder {
    public Builder setPreserveNewlines(boolean value);
    public Builder setPreserveListPrefixes(boolean value);
    public Builder setPreserveBlockquotes(boolean value);
    public Builder setNormalizeCustomBlockTags(boolean value);
    public Builder setProtectAutolinks(boolean value);
    public Builder setEnableRegexFallbackProtection(boolean value);
    public Builder setPreserveWhitespaceAroundProtectedSegments(boolean value);
    public Builder setEscapedMarkdownCharactersToProtect(String characters);
    public Builder setTokenMarker(String marker);
    public Builder setMaxCharsPerChunk(int value);
    public Builder setTranslationTimingListener(@Nullable TranslationTimingListener listener);
    public MarkdownTranslationOptions build();
  }
}
```

#### Option behavior notes

- `normalizeCustomBlockTags`
  - `true` (default): `<block>...</block>` is normalized to fenced code blocks before preparation.
  - `false`: custom block tags are left unchanged.
- `protectAutolinks`
  - controls whether autolinks are treated as protected spans in AST tokenization.
- `enableRegexFallbackProtection`
  - `true` (default): regex protection/restoration pipeline runs in fallback mode.
  - `false`: fallback mode skips regex token protection and passes normalized text directly.
- `escapedMarkdownCharactersToProtect`
  - controls which backslash-escaped Markdown punctuation pairs are protected from translation.
  - default: ``\\`*[]()#+-.!|>`` (does not include `_`, `{`, or `}`).
  - for example, include `[` and `]` to preserve `\[` and `\]` without ML Kit inserting spaces.
  - empty string disables this escape-specific protection.
- `tokenMarker`
  - marker fence used for AST chunk token boundaries.
  - default: `@@` (markers look like `@@MLMD_TOKEN_<id>@@`).
  - builder rejects empty string.
- `maxCharsPerChunk`
  - maximum number of plaintext characters included in each chunk sent to the translation engine.
  - default: `400`; the number matches the chunking behavior prior to exposing this option.
  - builder rejects values that are not greater than zero.

## Callback contracts

### `TranslationCallback`

```java
public interface TranslationCallback {
  void onSuccess(String translatedText);
  void onFailure(Exception error);
}
```

Failure note:

- Translation failures may return `TranslationException`.
- Use `TranslationException#getCode()` to branch on stable error categories like
  `TranslationErrorCode.MODEL_NOT_DOWNLOADED`.

### `TranslationErrorCode`

```java
public enum TranslationErrorCode {
  MODEL_NOT_DOWNLOADED,
  TIMEOUT,
  UNKNOWN
}
```

### `TranslationException`

```java
public final class TranslationException extends Exception {
  public TranslationErrorCode getCode();
}
```

## Translation metrics API

### `TranslationTimingListener`

```java
public interface TranslationTimingListener {
  void onCompleted(TranslationTimingReport report);
}
```

### `TranslationTimingReport`

```java
public final class TranslationTimingReport {
  public ProcessingMode getProcessingMode();
  public long getPreparationDurationMs();
  public long getTranslationDurationMs();
  public long getRestorationDurationMs();
  public long getTotalDurationMs();
  public int getTotalTokenCount();
  public int getTotalChunkCount();
  public int getChunkParseRecoveryCount();
  public boolean isRegexFallbackTriggered();
  public boolean isSuccessful();
  public @Nullable Exception getError();
}
```

## Internal/implementation classes

These are part of implementation packages and may change:

- `MlKitTranslationEngine`
- `DefaultMarkdownTranslator`
- Markdown pipeline/tokenization classes under `markdown/`
