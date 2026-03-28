package io.github.godsarmy.mlmarkdown.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationErrorCode;
import io.github.godsarmy.mlmarkdown.api.TranslationException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MlKitTranslationEngineAndroidTest {
    @Test
    public void translate_invokesCallbacksThroughInjectedClient() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", callback);

        assertEquals(1, factory.createCount);
        assertEquals(1, factory.client.translateCount);
        assertEquals("OK:hello", callback.translatedText);
    }

    @Test
    public void translate_propagatesTranslationFailure() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        factory.client.translateError = new IllegalStateException("model missing");
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", callback);

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
    }

    private static final class FakeTranslatorClientFactory
            implements MlKitTranslationEngine.TranslatorClientFactory {
        private int createCount;
        private final FakeTranslatorClient client = new FakeTranslatorClient();

        @Override
        public MlKitTranslationEngine.TranslatorClient create(
                String sourceLanguage, String targetLanguage) {
            createCount++;
            return client;
        }
    }

    private static final class FakeTranslatorClient
            implements MlKitTranslationEngine.TranslatorClient {
        private int translateCount;
        private Exception translateError;

        @Override
        public void translate(String text, TranslationCallback callback) {
            if (translateError != null) {
                callback.onFailure(translateError);
                return;
            }
            translateCount++;
            callback.onSuccess("OK:" + text);
        }

        @Override
        public void close() {
            // no-op for fake
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
