package io.github.godsarmy.mlmarkdown.api;

public interface MarkdownTranslator {
    void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback);
}
