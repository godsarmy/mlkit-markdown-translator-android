package io.github.godsarmy.mlmarkdown.api;

public interface LanguageModelManager {
    void ensureModelDownloaded(String targetLanguage, OperationCallback callback);

    void getDownloadedModels(LanguagePacksCallback callback);

    void deleteModel(String languageCode, OperationCallback callback);
}
