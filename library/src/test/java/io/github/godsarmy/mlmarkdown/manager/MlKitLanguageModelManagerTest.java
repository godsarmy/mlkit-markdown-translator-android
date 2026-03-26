package io.github.godsarmy.mlmarkdown.manager;

import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MlKitLanguageModelManagerTest {
    @Test
    public void ensureModelDownloaded_downloadsNormalizedLanguage() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestOperationCallback callback = new TestOperationCallback();

        manager.ensureModelDownloaded("es-ES", callback);

        assertEquals("es", remoteClient.downloadedLanguage);
        assertNotNull(callback.successCalled);
    }

    @Test
    public void ensureModelDownloaded_treatsEnglishAsBuiltIn() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestOperationCallback callback = new TestOperationCallback();

        manager.ensureModelDownloaded("en", callback);

        assertNull(remoteClient.downloadedLanguage);
        assertNotNull(callback.successCalled);
    }

    @Test
    public void getDownloadedModels_returnsSortedLanguages() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        remoteClient.downloadedModels = new LinkedHashSet<>(Set.of("fr", "es", "de"));
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestLanguagePacksCallback callback = new TestLanguagePacksCallback();

        manager.getDownloadedModels(callback);

        assertEquals(List.of("de", "es", "fr"), callback.languageCodes);
    }

    @Test
    public void deleteModel_deletesNormalizedLanguage() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestOperationCallback callback = new TestOperationCallback();

        manager.deleteModel("pt-BR", callback);

        assertEquals("pt", remoteClient.deletedLanguage);
        assertNotNull(callback.successCalled);
    }

    @Test
    public void deleteModel_propagatesFailure() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        remoteClient.deleteError = new IllegalStateException("cannot delete");
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestOperationCallback callback = new TestOperationCallback();

        manager.deleteModel("fr", callback);

        assertNotNull(callback.error);
        assertEquals("cannot delete", callback.error.getMessage());
    }

    private static final class FakeRemoteModelManagerClient implements MlKitLanguageModelManager.RemoteModelManagerClient {
        private String downloadedLanguage;
        private String deletedLanguage;
        private Exception deleteError;
        private Set<String> downloadedModels = Set.of();

        @Override
        public void downloadModel(String languageCode, OperationCallback callback) {
            downloadedLanguage = languageCode;
            callback.onSuccess();
        }

        @Override
        public void getDownloadedModels(MlKitLanguageModelManager.DownloadedModelsCallback callback) {
            callback.onSuccess(downloadedModels);
        }

        @Override
        public void deleteModel(String languageCode, OperationCallback callback) {
            deletedLanguage = languageCode;
            if (deleteError != null) {
                callback.onFailure(deleteError);
                return;
            }
            callback.onSuccess();
        }
    }

    private static final class TestOperationCallback implements OperationCallback {
        private Boolean successCalled;
        private Exception error;

        @Override
        public void onSuccess() {
            successCalled = Boolean.TRUE;
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }

    private static final class TestLanguagePacksCallback implements LanguagePacksCallback {
        private List<String> languageCodes;
        private Exception error;

        @Override
        public void onSuccess(List<String> languageCodes) {
            this.languageCodes = languageCodes;
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }
}
