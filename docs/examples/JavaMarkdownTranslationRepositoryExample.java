package io.github.godsarmy.mlmarkdown.examples;

import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

/**
 * Example repository-style wrapper for Java-based Android apps.
 *
 * Host apps can call this class from a ViewModel/Presenter and keep UI concerns
 * separate from translation orchestration.
 *
 * Note: language-model lifecycle is now managed by host app code via native ML Kit APIs.
 */
public final class JavaMarkdownTranslationRepositoryExample {
    private final MlKitMarkdownTranslator translator;

    public JavaMarkdownTranslationRepositoryExample() {
        this.translator = new MlKitMarkdownTranslator();
    }

    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        translator.translateMarkdown(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void close() {
        translator.close();
    }
}
