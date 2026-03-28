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

  public void ensureLanguageModelDownloaded(String targetLanguage, OperationCallback callback);
  public void getDownloadedLanguagePacks(LanguagePacksCallback callback);
  public void deleteLanguagePack(String languageCode, OperationCallback callback);

  @Override
  public void close();
}
```

### Usage notes

- Call `ensureLanguageModelDownloaded(...)` before first translation for a target language.
- Reuse one `MlKitMarkdownTranslator` instance per screen/controller scope.
- Call `close()` when owner is destroyed.

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
  public @Nullable TranslationTimingListener translationTimingListener();

  public static final class Builder {
    public Builder setPreserveNewlines(boolean value);
    public Builder setPreserveListPrefixes(boolean value);
    public Builder setPreserveBlockquotes(boolean value);
    public Builder setNormalizeCustomBlockTags(boolean value);
    public Builder setProtectAutolinks(boolean value);
    public Builder setEnableRegexFallbackProtection(boolean value);
    public Builder setPreserveWhitespaceAroundProtectedSegments(boolean value);
    public Builder setTranslationTimingListener(@Nullable TranslationTimingListener listener);
    public MarkdownTranslationOptions build();
  }
}
```

## Callback contracts

### `TranslationCallback`

```java
public interface TranslationCallback {
  void onSuccess(String translatedText);
  void onFailure(Exception error);
}
```

### `OperationCallback`

```java
public interface OperationCallback {
  void onSuccess();
  void onFailure(Exception error);
}
```

### `LanguagePacksCallback`

```java
public interface LanguagePacksCallback {
  void onSuccess(List<String> languageCodes);
  void onFailure(Exception error);
}
```

## Timing/metrics API

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
  public boolean isSuccessful();
  public @Nullable Exception getError();
}
```

## Internal/implementation classes

These are part of implementation packages and may change:

- `MlKitTranslationEngine`
- `DefaultMarkdownTranslator`
- `MlKitLanguageModelManager`
- Markdown pipeline/tokenization classes under `markdown/`
