package io.github.godsarmy.mlmarkdown.manager;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import io.github.godsarmy.mlmarkdown.api.LanguageModelManager;
import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MlKitLanguageModelManager implements LanguageModelManager {
    private final RemoteModelManagerClient remoteModelManagerClient;

    public MlKitLanguageModelManager() {
        this(
                new DefaultRemoteModelManagerClient(
                        RemoteModelManager.getInstance(),
                        new DownloadConditions.Builder().build()));
    }

    MlKitLanguageModelManager(RemoteModelManagerClient remoteModelManagerClient) {
        this.remoteModelManagerClient = remoteModelManagerClient;
    }

    @Override
    public void ensureModelDownloaded(String targetLanguage, OperationCallback callback) {
        String normalizedLanguage = normalizeLanguageCode(targetLanguage);
        if (normalizedLanguage == null) {
            callback.onFailure(new IllegalArgumentException("Unsupported ML Kit language code"));
            return;
        }

        if (TranslateLanguage.ENGLISH.equals(normalizedLanguage)) {
            callback.onSuccess();
            return;
        }

        remoteModelManagerClient.downloadModel(normalizedLanguage, callback);
    }

    @Override
    public void getDownloadedModels(LanguagePacksCallback callback) {
        remoteModelManagerClient.getDownloadedModels(
                new DownloadedModelsCallback() {
                    @Override
                    public void onSuccess(Set<String> languageCodes) {
                        List<String> codes = new ArrayList<>(languageCodes);
                        Collections.sort(codes);
                        callback.onSuccess(codes);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error);
                    }
                });
    }

    @Override
    public void deleteModel(String languageCode, OperationCallback callback) {
        String normalizedLanguage = normalizeLanguageCode(languageCode);
        if (normalizedLanguage == null) {
            callback.onFailure(new IllegalArgumentException("Unsupported ML Kit language code"));
            return;
        }

        if (TranslateLanguage.ENGLISH.equals(normalizedLanguage)) {
            callback.onSuccess();
            return;
        }

        remoteModelManagerClient.deleteModel(normalizedLanguage, callback);
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

    interface RemoteModelManagerClient {
        void downloadModel(String languageCode, OperationCallback callback);

        void getDownloadedModels(DownloadedModelsCallback callback);

        void deleteModel(String languageCode, OperationCallback callback);
    }

    interface DownloadedModelsCallback {
        void onSuccess(Set<String> languageCodes);

        void onFailure(Exception error);
    }

    private static final class DefaultRemoteModelManagerClient implements RemoteModelManagerClient {
        private final RemoteModelManager remoteModelManager;
        private final DownloadConditions downloadConditions;

        private DefaultRemoteModelManagerClient(
                RemoteModelManager remoteModelManager, DownloadConditions downloadConditions) {
            this.remoteModelManager = remoteModelManager;
            this.downloadConditions = downloadConditions;
        }

        @Override
        public void downloadModel(String languageCode, OperationCallback callback) {
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(languageCode).build();
            remoteModelManager
                    .download(model, downloadConditions)
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(error -> callback.onFailure(error));
        }

        @Override
        public void getDownloadedModels(DownloadedModelsCallback callback) {
            remoteModelManager
                    .getDownloadedModels(TranslateRemoteModel.class)
                    .addOnSuccessListener(
                            models -> {
                                Set<String> languageCodes = new java.util.LinkedHashSet<>();
                                for (TranslateRemoteModel model : models) {
                                    languageCodes.add(model.getLanguage());
                                }
                                callback.onSuccess(languageCodes);
                            })
                    .addOnFailureListener(callback::onFailure);
        }

        @Override
        public void deleteModel(String languageCode, OperationCallback callback) {
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(languageCode).build();
            remoteModelManager
                    .deleteDownloadedModel(model)
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(error -> callback.onFailure(error));
        }
    }
}
