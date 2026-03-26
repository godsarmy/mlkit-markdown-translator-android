package io.github.godsarmy.mlmarkdown.engine;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;

public class MlKitTranslationEngine implements TranslationEngine, Closeable {
    private final TranslatorClientFactory translatorClientFactory;
    private final Map<String, TranslatorClient> translatorsByLanguagePair = new LinkedHashMap<>();

    public MlKitTranslationEngine() {
        this(new DefaultTranslatorClientFactory());
    }

    MlKitTranslationEngine(TranslatorClientFactory translatorClientFactory) {
        this.translatorClientFactory = translatorClientFactory;
    }

    @Override
    public void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        if (text == null) {
            callback.onFailure(new IllegalArgumentException("Text must not be null"));
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
        translator.downloadModelIfNeeded(new OperationCallback() {
            @Override
            public void onSuccess() {
                translator.translate(text, callback);
            }

            @Override
            public void onFailure(Exception error) {
                callback.onFailure(error);
            }
        });
    }

    @Override
    public void close() {
        for (TranslatorClient translator : translatorsByLanguagePair.values()) {
            translator.close();
        }
        translatorsByLanguagePair.clear();
    }

    private TranslatorClient getOrCreateTranslator(String sourceLanguage, String targetLanguage) {
        String key = sourceLanguage + "->" + targetLanguage;
        TranslatorClient existingTranslator = translatorsByLanguagePair.get(key);
        if (existingTranslator != null) {
            return existingTranslator;
        }

        TranslatorClient createdTranslator = translatorClientFactory.create(sourceLanguage, targetLanguage);
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

    interface TranslatorClientFactory {
        TranslatorClient create(String sourceLanguage, String targetLanguage);
    }

    interface TranslatorClient extends Closeable {
        void downloadModelIfNeeded(OperationCallback callback);

        void translate(String text, TranslationCallback callback);

        @Override
        void close();
    }

    private static final class DefaultTranslatorClientFactory implements TranslatorClientFactory {
        @Override
        public TranslatorClient create(String sourceLanguage, String targetLanguage) {
            TranslatorOptions options = new TranslatorOptions.Builder()
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
        public void downloadModelIfNeeded(OperationCallback callback) {
            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(error -> callback.onFailure(asException(error)));
        }

        @Override
        public void translate(String text, TranslationCallback callback) {
            translator.translate(text)
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
