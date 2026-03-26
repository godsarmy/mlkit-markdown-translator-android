package io.github.godsarmy.mlmarkdown.engine;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

/**
 * Android/ML Kit translation adapter placeholder.
 */
public class MlKitTranslationEngine implements TranslationEngine {
    @Override
    public void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        callback.onFailure(new UnsupportedOperationException("ML Kit adapter not implemented yet"));
    }
}
