package io.github.godsarmy.mlmarkdown.examples;

import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

import java.util.List;

/**
 * Example repository-style wrapper for Java-based Android apps.
 *
 * Host apps can call this class from a ViewModel/Presenter and keep UI concerns
 * separate from translation/model-management orchestration.
 */
public final class JavaMarkdownTranslationRepositoryExample {
    private final MlKitMarkdownTranslator translator;

    public JavaMarkdownTranslationRepositoryExample() {
        this.translator = new MlKitMarkdownTranslator();
    }

    public void ensureModel(String targetLanguage, OperationCallback callback) {
        translator.ensureLanguageModelDownloaded(targetLanguage, callback);
    }

    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        translator.translateMarkdown(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void getDownloadedLanguagePacks(LanguagePacksCallback callback) {
        translator.getDownloadedLanguagePacks(callback);
    }

    public void deleteLanguagePack(String languageCode, OperationCallback callback) {
        translator.deleteLanguagePack(languageCode, callback);
    }

    public void close() {
        translator.close();
    }

    // Example callback implementation pattern for Java callers.
    private static final class LoggingLanguagePacksCallback implements LanguagePacksCallback {
        @Override
        public void onSuccess(List<String> languageCodes) {
            // Integrate with your logger/telemetry in app code.
        }

        @Override
        public void onFailure(Exception error) {
            // Surface to app-level error handling.
        }
    }
}
