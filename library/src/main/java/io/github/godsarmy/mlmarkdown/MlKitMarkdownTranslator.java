package io.github.godsarmy.mlmarkdown;

import io.github.godsarmy.mlmarkdown.api.LanguageModelManager;
import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.MlKitTranslationEngine;
import io.github.godsarmy.mlmarkdown.manager.MlKitLanguageModelManager;
import io.github.godsarmy.mlmarkdown.markdown.DefaultMarkdownTranslator;
import java.io.Closeable;

public final class MlKitMarkdownTranslator implements Closeable {
    private final MarkdownTranslator markdownTranslator;
    private final LanguageModelManager languageModelManager;
    private final Closeable closeableResource;

    public MlKitMarkdownTranslator() {
        this(MarkdownTranslationOptions.defaults());
    }

    public MlKitMarkdownTranslator(MarkdownTranslationOptions options) {
        this(new MlKitTranslationEngine(), options);
    }

    private MlKitMarkdownTranslator(
            MlKitTranslationEngine translationEngine, MarkdownTranslationOptions options) {
        this(
                new DefaultMarkdownTranslator(translationEngine, options),
                new MlKitLanguageModelManager(),
                translationEngine);
    }

    MlKitMarkdownTranslator(
            MarkdownTranslator markdownTranslator,
            LanguageModelManager languageModelManager,
            Closeable closeableResource) {
        this.markdownTranslator = markdownTranslator;
        this.languageModelManager = languageModelManager;
        this.closeableResource = closeableResource;
    }

    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        markdownTranslator.translateMarkdown(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void ensureLanguageModelDownloaded(String targetLanguage, OperationCallback callback) {
        languageModelManager.ensureModelDownloaded(targetLanguage, callback);
    }

    public void getDownloadedLanguagePacks(LanguagePacksCallback callback) {
        languageModelManager.getDownloadedModels(callback);
    }

    public void deleteLanguagePack(String languageCode, OperationCallback callback) {
        languageModelManager.deleteModel(languageCode, callback);
    }

    @Override
    public void close() {
        try {
            closeableResource.close();
        } catch (Exception ignored) {
            // No-op for the simple facade close path.
        }
    }
}
