package io.github.godsarmy.mlmarkdown.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationErrorCode;
import io.github.godsarmy.mlmarkdown.api.TranslationException;
import org.junit.Test;

public class MlKitTranslationEngineTest {
    @Test
    public void translate_usesCachedTranslatorClient() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback firstCallback = new TestTranslationCallback();
        TestTranslationCallback secondCallback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", firstCallback);
        engine.translate("world", "en", "es", secondCallback);

        assertEquals(1, factory.createCount);
        assertEquals(2, factory.client.translateCount);
        assertEquals("ML(hello)", firstCallback.translatedText);
        assertEquals("ML(world)", secondCallback.translatedText);
    }

    @Test
    public void translate_returnsInputWhenLanguagesMatch() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("already same", "en", "en", callback);

        assertEquals("already same", callback.translatedText);
        assertEquals(0, factory.createCount);
    }

    @Test
    public void translate_propagatesTranslationFailure() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        factory.client.translateError = new IllegalStateException("model missing");
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", callback);

        assertNull(callback.translatedText);
        assertNotNull(callback.error);
        assertEquals("model missing", callback.error.getMessage());
    }

    @Test
    public void translate_mapsModelNotDownloadedError() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        factory.client.translateError =
                new IllegalStateException("Please download the model before translating");
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", callback);

        assertNotNull(callback.error);
        assertTrue(callback.error instanceof TranslationException);
        TranslationException translationException = (TranslationException) callback.error;
        assertEquals(TranslationErrorCode.MODEL_NOT_DOWNLOADED, translationException.getCode());
        assertNotNull(translationException.getCause());
    }

    @Test
    public void close_closesCachedTranslators() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);

        engine.translate("hello", "en", "es", new TestTranslationCallback());
        engine.close();

        assertEquals(1, factory.client.closeCount);
    }

    private static final class FakeTranslatorClientFactory
            implements MlKitTranslationEngine.TranslatorClientFactory {
        private int createCount;
        private final FakeTranslatorClient client = new FakeTranslatorClient();

        @Override
        public MlKitTranslationEngine.TranslatorClient create(
                String sourceLanguage, String targetLanguage) {
            createCount++;
            client.lastSourceLanguage = sourceLanguage;
            client.lastTargetLanguage = targetLanguage;
            return client;
        }
    }

    private static final class FakeTranslatorClient
            implements MlKitTranslationEngine.TranslatorClient {
        private int translateCount;
        private int closeCount;
        private Exception translateError;
        private String lastSourceLanguage;
        private String lastTargetLanguage;

        @Override
        public void translate(String text, TranslationCallback callback) {
            if (translateError != null) {
                callback.onFailure(translateError);
                return;
            }
            translateCount++;
            callback.onSuccess("ML(" + text + ")");
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    private static final class TestTranslationCallback implements TranslationCallback {
        private String translatedText;
        private Exception error;

        @Override
        public void onSuccess(String translatedText) {
            this.translatedText = translatedText;
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }
}
