package io.github.godsarmy.mlmarkdown.manager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback;
import io.github.godsarmy.mlmarkdown.api.OperationCallback;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MlKitLanguageModelManagerAndroidTest {
    @Test
    public void getDownloadedModels_returnsSortedList() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        remoteClient.downloadedModels = new LinkedHashSet<>(Set.of("fr", "de", "es"));
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestLanguagePacksCallback callback = new TestLanguagePacksCallback();

        manager.getDownloadedModels(callback);

        assertEquals(List.of("de", "es", "fr"), callback.languageCodes);
    }

    @Test
    public void deleteModel_propagatesFailure() {
        FakeRemoteModelManagerClient remoteClient = new FakeRemoteModelManagerClient();
        remoteClient.deleteError = new IllegalStateException("delete failed");
        MlKitLanguageModelManager manager = new MlKitLanguageModelManager(remoteClient);
        TestOperationCallback callback = new TestOperationCallback();

        manager.deleteModel("fr", callback);

        assertNotNull(callback.error);
        assertEquals("delete failed", callback.error.getMessage());
    }

    private static final class FakeRemoteModelManagerClient implements MlKitLanguageModelManager.RemoteModelManagerClient {
        private Set<String> downloadedModels = Set.of();
        private Exception deleteError;

        @Override
        public void downloadModel(String languageCode, OperationCallback callback) {
            callback.onSuccess();
        }

        @Override
        public void getDownloadedModels(MlKitLanguageModelManager.DownloadedModelsCallback callback) {
            callback.onSuccess(downloadedModels);
        }

        @Override
        public void deleteModel(String languageCode, OperationCallback callback) {
            if (deleteError != null) {
                callback.onFailure(deleteError);
                return;
            }
            callback.onSuccess();
        }
    }

    private static final class TestOperationCallback implements OperationCallback {
        private Exception error;

        @Override
        public void onSuccess() {
            // no-op
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }

    private static final class TestLanguagePacksCallback implements LanguagePacksCallback {
        private List<String> languageCodes;

        @Override
        public void onSuccess(List<String> languageCodes) {
            this.languageCodes = languageCodes;
        }

        @Override
        public void onFailure(Exception error) {
            throw new AssertionError(error);
        }
    }
}
