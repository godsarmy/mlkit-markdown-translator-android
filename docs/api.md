# Public API Draft (Step 4)

This file defines the initial public API surface before full implementation.

## Translation callback contracts

```java
public interface TranslationCallback {
  void onSuccess(String translatedText);
  void onFailure(Exception error);
}
```

```java
public interface OperationCallback {
  void onSuccess();
  void onFailure(Exception error);
}
```

```java
public interface LanguagePacksCallback {
  void onSuccess(List<String> languageCodes);
  void onFailure(Exception error);
}
```

## Translation engine abstraction

```java
public interface TranslationEngine {
  void translate(
      String text,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);
}
```

## Markdown translator facade

```java
public interface MarkdownTranslator {
  void translateMarkdown(
      String markdown,
      String sourceLanguage,
      String targetLanguage,
      TranslationCallback callback);
}
```

## Language model management facade

```java
public interface LanguageModelManager {
  void ensureModelDownloaded(String targetLanguage, OperationCallback callback);
  void getDownloadedModels(LanguagePacksCallback callback);
  void deleteModel(String languageCode, OperationCallback callback);
}
```

## Planned concrete classes

- `MlKitTranslationEngine`
- `DefaultMarkdownTranslator`
- `MlKitLanguageModelManager`
