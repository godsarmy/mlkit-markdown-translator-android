package io.github.godsarmy.mlmarkdown.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
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
        assertEquals(1, factory.client.downloadCount);
        assertEquals(1, factory.client.translateCount);
        assertEquals("OK:hello", callback.translatedText);
    }

    @Test
    public void translate_propagatesDownloadFailure() {
        FakeTranslatorClientFactory factory = new FakeTranslatorClientFactory();
        factory.client.downloadError = new IllegalStateException("download failed");
        MlKitTranslationEngine engine = new MlKitTranslationEngine(factory);
        TestTranslationCallback callback = new TestTranslationCallback();

        engine.translate("hello", "en", "es", callback);

        assertNotNull(callback.error);
        assertEquals("download failed", callback.error.getMessage());
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
        private int downloadCount;
        private int translateCount;
        private Exception downloadError;

        @Override
        public void downloadModelIfNeeded(OperationCallback callback) {
            downloadCount++;
            if (downloadError != null) {
                callback.onFailure(downloadError);
                return;
            }
            callback.onSuccess();
        }

        @Override
        public void translate(String text, TranslationCallback callback) {
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
