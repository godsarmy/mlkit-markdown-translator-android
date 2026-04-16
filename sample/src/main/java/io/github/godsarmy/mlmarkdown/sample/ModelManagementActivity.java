package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModelManagementActivity extends AppCompatActivity {
    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
    private final DownloadConditions downloadConditions = new DownloadConditions.Builder().build();
    private final List<String> supportedLanguages = new ArrayList<>();
    private final Set<String> downloadedModels = new HashSet<>();
    private final List<String> availableModels = new ArrayList<>();
    private final List<String> downloadedModelsList = new ArrayList<>();

    private ListView availableModelsListView;
    private ListView downloadedModelsListView;
    private Button downloadButton;
    private Button deleteButton;
    private TextView availableModelsEmptyText;
    private TextView downloadedModelsEmptyText;
    private TextView modelStatusText;
    private View operationProgress;
    private ArrayAdapter<String> availableModelsAdapter;
    private ArrayAdapter<String> downloadedModelsAdapter;
    @Nullable private String selectedAvailableLanguage;
    @Nullable private String selectedDownloadedLanguage;
    private boolean isBusy;

    public static Intent createIntent(Context context) {
        return new Intent(context, ModelManagementActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_management);
        bindViews();
        setupLanguageLists();
        setupActions();
        refreshDownloadedModels();
    }

    private void bindViews() {
        availableModelsListView = findViewById(R.id.availableModelsList);
        downloadedModelsListView = findViewById(R.id.downloadedModelsList);
        downloadButton = findViewById(R.id.downloadTargetModelButton);
        deleteButton = findViewById(R.id.deleteTargetModelButton);
        availableModelsEmptyText = findViewById(R.id.availableModelsEmptyText);
        downloadedModelsEmptyText = findViewById(R.id.downloadedModelsEmptyText);
        modelStatusText = findViewById(R.id.modelStatusText);
        operationProgress = findViewById(R.id.modelOperationProgress);

        Collections.addAll(
                supportedLanguages, getResources().getStringArray(R.array.language_codes));
    }

    private void setupLanguageLists() {
        availableModelsAdapter =
                new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_single_choice, availableModels);
        downloadedModelsAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_single_choice,
                        downloadedModelsList);
        availableModelsListView.setAdapter(availableModelsAdapter);
        downloadedModelsListView.setAdapter(downloadedModelsAdapter);
    }

    private void setupActions() {
        availableModelsListView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedAvailableLanguage = availableModels.get(position);
                    updateUiState();
                });

        downloadedModelsListView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedDownloadedLanguage = downloadedModelsList.get(position);
                    updateUiState();
                });

        downloadButton.setOnClickListener(v -> downloadTargetModel());
        deleteButton.setOnClickListener(v -> deleteTargetModel());
    }

    private void refreshDownloadedModels() {
        remoteModelManager
                .getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(
                        models ->
                                runOnUiThread(
                                        () -> {
                                            downloadedModels.clear();
                                            for (TranslateRemoteModel model : models) {
                                                downloadedModels.add(model.getLanguage());
                                            }
                                            rebuildLanguageLists();
                                            updateUiState();
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            rebuildLanguageLists();
                                            updateUiState();
                                        }));
    }

    private void rebuildLanguageLists() {
        availableModels.clear();
        downloadedModelsList.clear();

        for (String language : supportedLanguages) {
            String normalized = normalizeLanguageCode(language);
            if (normalized != null && downloadedModels.contains(normalized)) {
                downloadedModelsList.add(language);
            } else {
                availableModels.add(language);
            }
        }

        if (selectedAvailableLanguage == null
                || !availableModels.contains(selectedAvailableLanguage)) {
            selectedAvailableLanguage = availableModels.isEmpty() ? null : availableModels.get(0);
        }
        if (selectedDownloadedLanguage == null
                || !downloadedModelsList.contains(selectedDownloadedLanguage)) {
            selectedDownloadedLanguage =
                    downloadedModelsList.isEmpty() ? null : downloadedModelsList.get(0);
        }

        availableModelsAdapter.notifyDataSetChanged();
        downloadedModelsAdapter.notifyDataSetChanged();
        updateListSelection(availableModelsListView, availableModels, selectedAvailableLanguage);
        updateListSelection(
                downloadedModelsListView, downloadedModelsList, selectedDownloadedLanguage);
    }

    private static void updateListSelection(
            ListView listView, List<String> values, @Nullable String selectedValue) {
        listView.clearChoices();
        if (selectedValue == null) {
            return;
        }
        int index = values.indexOf(selectedValue);
        if (index >= 0) {
            listView.setItemChecked(index, true);
        }
    }

    private void setBusy(boolean busy) {
        isBusy = busy;
        updateUiState();
    }

    private void updateUiState() {
        boolean hasAvailableSelection = selectedAvailableLanguage != null;
        boolean hasDownloadedSelection = selectedDownloadedLanguage != null;

        operationProgress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        availableModelsListView.setEnabled(!isBusy);
        downloadedModelsListView.setEnabled(!isBusy);
        downloadButton.setEnabled(!isBusy && hasAvailableSelection);
        deleteButton.setEnabled(!isBusy && hasDownloadedSelection);
        availableModelsEmptyText.setVisibility(
                availableModels.isEmpty() ? View.VISIBLE : View.GONE);
        downloadedModelsEmptyText.setVisibility(
                downloadedModelsList.isEmpty() ? View.VISIBLE : View.GONE);

        modelStatusText.setText(
                getString(
                        R.string.model_management_status,
                        downloadedModelsList.size(),
                        availableModels.size()));
    }

    private void downloadTargetModel() {
        if (selectedAvailableLanguage == null) {
            return;
        }
        String language = selectedAvailableLanguage;
        String normalizedLanguageCode = normalizeLanguageCode(language);
        if (normalizedLanguageCode == null) {
            return;
        }

        setBusy(true);
        TranslateRemoteModel model =
                new TranslateRemoteModel.Builder(normalizedLanguageCode).build();
        remoteModelManager
                .download(model, downloadConditions)
                .addOnSuccessListener(
                        unused ->
                                runOnUiThread(
                                        () -> {
                                            setBusy(false);
                                            refreshDownloadedModels();
                                            Toast.makeText(
                                                            this,
                                                            getString(
                                                                    R.string
                                                                            .model_download_success_message,
                                                                    language),
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            setBusy(false);
                                            Toast.makeText(
                                                            this,
                                                            getString(
                                                                    R.string
                                                                            .model_download_failed_message,
                                                                    language),
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }));
    }

    private void deleteTargetModel() {
        if (selectedDownloadedLanguage == null) {
            return;
        }
        String language = selectedDownloadedLanguage;
        String normalizedLanguageCode = normalizeLanguageCode(language);
        if (normalizedLanguageCode == null) {
            return;
        }

        setBusy(true);
        TranslateRemoteModel model =
                new TranslateRemoteModel.Builder(normalizedLanguageCode).build();
        remoteModelManager
                .deleteDownloadedModel(model)
                .addOnSuccessListener(
                        unused ->
                                runOnUiThread(
                                        () -> {
                                            setBusy(false);
                                            refreshDownloadedModels();
                                            Toast.makeText(
                                                            this,
                                                            getString(
                                                                    R.string
                                                                            .model_delete_success_message,
                                                                    language),
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            setBusy(false);
                                            Toast.makeText(
                                                            this,
                                                            getString(
                                                                    R.string
                                                                            .model_delete_failed_message,
                                                                    language),
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }));
    }

    @Nullable
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
}
