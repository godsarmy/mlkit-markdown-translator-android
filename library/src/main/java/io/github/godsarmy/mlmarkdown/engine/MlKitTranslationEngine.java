package io.github.godsarmy.mlmarkdown.engine;

import androidx.annotation.NonNull;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationErrorCode;
import io.github.godsarmy.mlmarkdown.api.TranslationException;
import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MlKitTranslationEngine implements TranslationEngine, Closeable {
    private final TranslatorClientFactory translatorClientFactory;
    private final ScheduledExecutorService timeoutScheduler;
    private final Map<String, TranslatorClient> translatorsByLanguagePair = new LinkedHashMap<>();

    public MlKitTranslationEngine() {
        this(new DefaultTranslatorClientFactory(), newTimeoutScheduler());
    }

    MlKitTranslationEngine(TranslatorClientFactory translatorClientFactory) {
        this(translatorClientFactory, newTimeoutScheduler());
    }

    MlKitTranslationEngine(
            TranslatorClientFactory translatorClientFactory,
            ScheduledExecutorService timeoutScheduler) {
        this.translatorClientFactory = translatorClientFactory;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    public void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        translate(text, sourceLanguage, targetLanguage, 0, callback);
    }

    @Override
    public void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            long timeoutMs,
            TranslationCallback callback) {
        if (text == null) {
            callback.onFailure(new IllegalArgumentException("Text must not be null"));
            return;
        }
        if (timeoutMs < 0) {
            callback.onFailure(new IllegalArgumentException("timeoutMs must be >= 0"));
            return;
        }

        String normalizedSource = normalizeLanguageCode(sourceLanguage);
        String normalizedTarget = normalizeLanguageCode(targetLanguage);
        if (normalizedSource == null || normalizedTarget == null) {
            callback.onFailure(new IllegalArgumentException("Unsupported ML Kit language code"));
            return;
        }

        if (normalizedSource.equals(normalizedTarget)) {
            callback.onSuccess(text);
            return;
        }

        TranslatorClient translator = getOrCreateTranslator(normalizedSource, normalizedTarget);
        AtomicBoolean completed = new AtomicBoolean(false);
        ScheduledFuture<?> timeoutFuture = scheduleTimeout(timeoutMs, completed, callback);
        translator.translate(
                text,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        if (!completed.compareAndSet(false, true)) {
                            return;
                        }
                        cancelTimeout(timeoutFuture);
                        callback.onSuccess(translatedText);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        if (!completed.compareAndSet(false, true)) {
                            return;
                        }
                        cancelTimeout(timeoutFuture);
                        callback.onFailure(mapTranslationError(error));
                    }
                });
    }

    @Override
    public void close() {
        for (TranslatorClient translator : translatorsByLanguagePair.values()) {
            translator.close();
        }
        translatorsByLanguagePair.clear();
        timeoutScheduler.shutdownNow();
    }

    private ScheduledFuture<?> scheduleTimeout(
            long timeoutMs, AtomicBoolean completed, TranslationCallback callback) {
        if (timeoutMs == 0) {
            return null;
        }
        return timeoutScheduler.schedule(
                () -> {
                    if (!completed.compareAndSet(false, true)) {
                        return;
                    }
                    callback.onFailure(
                            new TranslationException(
                                    TranslationErrorCode.TIMEOUT,
                                    "Translation timed out after " + timeoutMs + " ms",
                                    new TimeoutException("Translation timed out")));
                },
                timeoutMs,
                TimeUnit.MILLISECONDS);
    }

    private static void cancelTimeout(ScheduledFuture<?> timeoutFuture) {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private static ScheduledExecutorService newTimeoutScheduler() {
        return Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull Runnable runnable) {
                        Thread thread = new Thread(runnable, "mlmd-translation-timeout");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
    }

    private TranslatorClient getOrCreateTranslator(String sourceLanguage, String targetLanguage) {
        String key = sourceLanguage + "->" + targetLanguage;
        TranslatorClient existingTranslator = translatorsByLanguagePair.get(key);
        if (existingTranslator != null) {
            return existingTranslator;
        }

        TranslatorClient createdTranslator =
                translatorClientFactory.create(sourceLanguage, targetLanguage);
        translatorsByLanguagePair.put(key, createdTranslator);
        return createdTranslator;
    }

    private static String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return null;
        }

        String normalizedInput = languageCode.trim();
        String translatedLanguage = TranslateLanguage.fromLanguageTag(normalizedInput);
        if (translatedLanguage != null) {
            return translatedLanguage;
        }

        int separatorIndex = normalizedInput.indexOf('-');
        if (separatorIndex < 0) {
            separatorIndex = normalizedInput.indexOf('_');
        }
        if (separatorIndex > 0) {
            return TranslateLanguage.fromLanguageTag(normalizedInput.substring(0, separatorIndex));
        }

        return null;
    }

    private static Exception mapTranslationError(Exception error) {
        if (isModelNotDownloadedError(error)) {
            return new TranslationException(
                    TranslationErrorCode.MODEL_NOT_DOWNLOADED,
                    "Required language model is not downloaded",
                    error);
        }
        return error;
    }

    private static boolean isModelNotDownloadedError(Exception error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("model")
                && (normalizedMessage.contains("not downloaded")
                        || normalizedMessage.contains("download the model")
                        || normalizedMessage.contains("model unavailable"));
    }

    interface TranslatorClientFactory {
        TranslatorClient create(String sourceLanguage, String targetLanguage);
    }

    interface TranslatorClient extends Closeable {
        void translate(String text, TranslationCallback callback);

        @Override
        void close();
    }

    private static final class DefaultTranslatorClientFactory implements TranslatorClientFactory {
        @Override
        public TranslatorClient create(String sourceLanguage, String targetLanguage) {
            TranslatorOptions options =
                    new TranslatorOptions.Builder()
                            .setSourceLanguage(sourceLanguage)
                            .setTargetLanguage(targetLanguage)
                            .build();
            return new MlKitTranslatorClient(Translation.getClient(options));
        }
    }

    private static final class MlKitTranslatorClient implements TranslatorClient {
        private final Translator translator;

        private MlKitTranslatorClient(Translator translator) {
            this.translator = translator;
        }

        @Override
        public void translate(String text, TranslationCallback callback) {
            translator
                    .translate(text)
                    .addOnSuccessListener(callback::onSuccess)
                    .addOnFailureListener(error -> callback.onFailure(asException(error)));
        }

        @Override
        public void close() {
            translator.close();
        }

        @NonNull
        private static Exception asException(Exception error) {
            return error;
        }
    }
}
