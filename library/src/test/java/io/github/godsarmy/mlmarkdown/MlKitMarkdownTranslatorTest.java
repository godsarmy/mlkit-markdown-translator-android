package io.github.godsarmy.mlmarkdown;

import io.github.godsarmy.mlmarkdown.api.LanguageModelManager;
import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

import org.junit.Test;

import java.io.Closeable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MlKitMarkdownTranslatorTest {
    @Test
    public void translateMarkdown_delegatesToMarkdownTranslator() {
        FakeMarkdownTranslator markdownTranslator = new FakeMarkdownTranslator();
        FakeLanguageModelManager languageModelManager = new FakeLanguageModelManager();
        MlKitMarkdownTranslator facade = new MlKitMarkdownTranslator(
                markdownTranslator,
                languageModelManager,
                new FakeCloseable()
        );
        TestTranslationCallback callback = new TestTranslationCallback();

        facade.translateMarkdown("# Hello", "en", "es", callback);

        assertEquals("# Hello", markdownTranslator.markdown);
        assertEquals("en", markdownTranslator.sourceLanguage);
        assertEquals("es", markdownTranslator.targetLanguage);
        assertEquals("OK:# Hello", callback.translatedText);
    }

    @Test
    public void modelMethods_delegateToLanguageModelManager() {
        FakeLanguageModelManager languageModelManager = new FakeLanguageModelManager();
        MlKitMarkdownTranslator facade = new MlKitMarkdownTranslator(
                new FakeMarkdownTranslator(),
                languageModelManager,
                new FakeCloseable()
        );
        TestOperationCallback operationCallback = new TestOperationCallback();
        TestLanguagePacksCallback packsCallback = new TestLanguagePacksCallback();

        facade.ensureLanguageModelDownloaded("fr", operationCallback);
        facade.getDownloadedLanguagePacks(packsCallback);
        facade.deleteLanguagePack("de", operationCallback);

        assertEquals("fr", languageModelManager.lastEnsuredLanguage);
        assertEquals("de", languageModelManager.lastDeletedLanguage);
        assertEquals(List.of("de", "fr"), packsCallback.languageCodes);
        assertTrue(operationCallback.successCount >= 2);
    }

    @Test
    public void close_closesUnderlyingResource() {
        FakeCloseable closeable = new FakeCloseable();
        MlKitMarkdownTranslator facade = new MlKitMarkdownTranslator(
                new FakeMarkdownTranslator(),
                new FakeLanguageModelManager(),
                closeable
        );

        facade.close();

        assertTrue(closeable.closed);
    }

    private static final class FakeMarkdownTranslator implements MarkdownTranslator {
        private String markdown;
        private String sourceLanguage;
        private String targetLanguage;

        @Override
        public void translateMarkdown(String markdown, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
            this.markdown = markdown;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            callback.onSuccess("OK:" + markdown);
        }
    }

    private static final class FakeLanguageModelManager implements LanguageModelManager {
        private String lastEnsuredLanguage;
        private String lastDeletedLanguage;

        @Override
        public void ensureModelDownloaded(String targetLanguage, OperationCallback callback) {
            lastEnsuredLanguage = targetLanguage;
            callback.onSuccess();
        }

        @Override
        public void getDownloadedModels(LanguagePacksCallback callback) {
            callback.onSuccess(List.of("de", "fr"));
        }

        @Override
        public void deleteModel(String languageCode, OperationCallback callback) {
            lastDeletedLanguage = languageCode;
            callback.onSuccess();
        }
    }

    private static final class FakeCloseable implements Closeable {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TestTranslationCallback implements TranslationCallback {
        private String translatedText;

        @Override
        public void onSuccess(String translatedText) {
            this.translatedText = translatedText;
        }

        @Override
        public void onFailure(Exception error) {
            throw new AssertionError(error);
        }
    }

    private static final class TestOperationCallback implements OperationCallback {
        private int successCount;

        @Override
        public void onSuccess() {
            successCount++;
        }

        @Override
        public void onFailure(Exception error) {
            throw new AssertionError(error);
        }
    }

    private static final class TestLanguagePacksCallback implements LanguagePacksCallback {
        private List<String> languageCodes;

        @Override
        public void onSuccess(List<String> languageCodes) {
            this.languageCodes = languageCodes;
        }

        @Override
        public void onFailure(Exception error) {
            throw new AssertionError(error);
        }
    }
}
