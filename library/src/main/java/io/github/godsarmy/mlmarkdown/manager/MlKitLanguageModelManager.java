package io.github.godsarmy.mlmarkdown.manager;

import io.github.godsarmy.mlmarkdown.api.LanguageModelManager;
import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;

import java.util.Collections;

/**
 * Android/ML Kit language model manager placeholder.
 */
public class MlKitLanguageModelManager implements LanguageModelManager {
    @Override
    public void ensureModelDownloaded(String targetLanguage, OperationCallback callback) {
        callback.onFailure(new UnsupportedOperationException("Model download is not implemented yet"));
    }

    @Override
    public void getDownloadedModels(LanguagePacksCallback callback) {
        callback.onSuccess(Collections.emptyList());
    }

    @Override
    public void deleteModel(String languageCode, OperationCallback callback) {
        callback.onFailure(new UnsupportedOperationException("Model deletion is not implemented yet"));
    }
}
