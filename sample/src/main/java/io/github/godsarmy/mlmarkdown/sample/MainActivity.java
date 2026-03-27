package io.github.godsarmy.mlmarkdown.sample;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashSet;
import java.util.Set;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;

import io.noties.markwon.Markwon;

public final class MainActivity extends AppCompatActivity {
    private MlKitMarkdownTranslator translator;
    private Markwon markwon;

    private EditText originalMarkdownInput;
    private EditText translatedMarkdownRaw;
    private TextView originalMarkdownRendered;
    private TextView translatedMarkdownRendered;
    private SwitchMaterial renderModeSwitch;
    private SwitchMaterial fallbackModeSwitch;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;
    private TextView statusText;
    private Button downloadModelButton;
    private Button translateButton;

    private boolean isBusy;
    private boolean isRenderMode;
    private boolean isFallbackModeEnabled = true;
    private int activeDownloadRequestId;
    private AlertDialog downloadProgressDialog;
    private final Set<String> downloadedTargetModels = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translator = new MlKitMarkdownTranslator();
        markwon = Markwon.create(this);

        bindViews();
        setupLanguageSpinners();
        setupActions();
    }

    private void bindViews() {
        originalMarkdownInput = findViewById(R.id.originalMarkdownInput);
        translatedMarkdownRaw = findViewById(R.id.translatedMarkdownRaw);
        originalMarkdownRendered = findViewById(R.id.originalMarkdownRendered);
        translatedMarkdownRendered = findViewById(R.id.translatedMarkdownRendered);
        renderModeSwitch = findViewById(R.id.renderModeSwitch);
        fallbackModeSwitch = findViewById(R.id.fallbackModeSwitch);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        statusText = findViewById(R.id.statusText);
        downloadModelButton = findViewById(R.id.downloadModelButton);
        translateButton = findViewById(R.id.translateButton);

        originalMarkdownRendered.setMovementMethod(new ScrollingMovementMethod());
        translatedMarkdownRendered.setMovementMethod(new ScrollingMovementMethod());
    }

    private void setupLanguageSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_codes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(adapter);
        targetLanguageSpinner.setAdapter(adapter);

        sourceLanguageSpinner.setSelection(0); // en
        targetLanguageSpinner.setSelection(1); // es
    }

    private void setupActions() {
        downloadModelButton.setOnClickListener(v -> onModelButtonClicked());
        translateButton.setOnClickListener(v -> translateMarkdown());

        targetLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshDownloadedModelsAndButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateDownloadButtonState();
            }
        });

        renderModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRenderMode = isChecked;
            applyRenderMode();
        });

        fallbackModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isFallbackModeEnabled = isChecked;
            recreateTranslator();
        });

        originalMarkdownInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isRenderMode) {
                    markwon.setMarkdown(originalMarkdownRendered, s.toString());
                }
            }
        });

        refreshDownloadedModelsAndButtonState();
        applyRenderMode();
    }

    private void applyRenderMode() {
        String original = originalMarkdownInput.getText().toString();
        String translated = translatedMarkdownRaw.getText().toString();

        if (isRenderMode) {
            originalMarkdownInput.setEnabled(false);
            originalMarkdownInput.setFocusable(false);
            originalMarkdownInput.setFocusableInTouchMode(false);
            originalMarkdownInput.setVisibility(View.GONE);
            originalMarkdownRendered.setVisibility(View.VISIBLE);

            translatedMarkdownRaw.setVisibility(View.GONE);
            translatedMarkdownRendered.setVisibility(View.VISIBLE);

            markwon.setMarkdown(originalMarkdownRendered, original);
            markwon.setMarkdown(translatedMarkdownRendered, translated);
            return;
        }

        originalMarkdownInput.setEnabled(true);
        originalMarkdownInput.setFocusable(true);
        originalMarkdownInput.setFocusableInTouchMode(true);
        originalMarkdownInput.setVisibility(View.VISIBLE);
        originalMarkdownRendered.setVisibility(View.GONE);

        translatedMarkdownRaw.setVisibility(View.VISIBLE);
        translatedMarkdownRendered.setVisibility(View.GONE);
    }

    private void setBusy(boolean busy) {
        isBusy = busy;
        translateButton.setEnabled(!busy);
        updateDownloadButtonState();
    }

    private void updateDownloadButtonState() {
        boolean downloaded = downloadedTargetModels.contains(targetLanguage());
        downloadModelButton.setEnabled(!isBusy);
        downloadModelButton.setText(downloaded ? R.string.delete_model : R.string.download_model);
    }

    private String sourceLanguage() {
        return String.valueOf(sourceLanguageSpinner.getSelectedItem());
    }

    private String targetLanguage() {
        return String.valueOf(targetLanguageSpinner.getSelectedItem());
    }

    private void onModelButtonClicked() {
        String language = targetLanguage();
        if (downloadedTargetModels.contains(language)) {
            confirmDeleteModel(language);
            return;
        }

        downloadTargetModel(language);
    }

    private void confirmDeleteModel(String languageCode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_model_dialog_title)
                .setMessage(getString(R.string.delete_model_dialog_message, languageCode))
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_model, (dialog, which) -> deleteTargetModel(languageCode))
                .show();
    }

    private void deleteTargetModel(String languageCode) {
        setBusy(true);
        statusText.setText(getString(R.string.status_deleting_model, languageCode));

        translator.deleteLanguagePack(languageCode, new io.github.godsarmy.mlmarkdown.api.OperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    downloadedTargetModels.remove(languageCode);
                    statusText.setText(getString(R.string.status_model_deleted, languageCode));
                    setBusy(false);
                });
            }

            @Override
            public void onFailure(Exception error) {
                runOnUiThread(() -> {
                    statusText.setText(getString(R.string.status_model_delete_failed, error.getMessage()));
                    setBusy(false);
                });
            }
        });
    }

    private void downloadTargetModel(String languageCode) {
        if (downloadedTargetModels.contains(languageCode)) {
            updateDownloadButtonState();
            return;
        }

        setBusy(true);
        statusText.setText(getString(R.string.status_downloading_model, languageCode));

        int requestId = ++activeDownloadRequestId;
        showDownloadProgressDialog(languageCode, requestId);

        translator.ensureLanguageModelDownloaded(languageCode, new io.github.godsarmy.mlmarkdown.api.OperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (requestId != activeDownloadRequestId) {
                        return;
                    }

                    activeDownloadRequestId = 0;
                    dismissDownloadProgressDialog();
                    downloadedTargetModels.add(languageCode);
                    statusText.setText(getString(R.string.status_model_ready, languageCode));
                    showModelDownloadedDialog(languageCode);
                    setBusy(false);
                });
            }

            @Override
            public void onFailure(Exception error) {
                runOnUiThread(() -> {
                    if (requestId != activeDownloadRequestId) {
                        return;
                    }

                    activeDownloadRequestId = 0;
                    dismissDownloadProgressDialog();
                    statusText.setText(getString(R.string.status_model_download_failed, error.getMessage()));
                    setBusy(false);
                });
            }
        });
    }

    private void showDownloadProgressDialog(String languageCode, int requestId) {
        dismissDownloadProgressDialog();
        downloadProgressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.model_download_progress_title)
                .setMessage(getString(R.string.model_download_progress_message, languageCode))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel_download, (dialog, which) -> cancelActiveDownload(requestId))
                .setOnCancelListener(dialog -> cancelActiveDownload(requestId))
                .create();
        downloadProgressDialog.show();
    }

    private void cancelActiveDownload(int requestId) {
        if (requestId != activeDownloadRequestId) {
            return;
        }
        activeDownloadRequestId = 0;
        dismissDownloadProgressDialog();
        statusText.setText(R.string.status_model_download_cancelled);
        setBusy(false);
    }

    private void dismissDownloadProgressDialog() {
        if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
            downloadProgressDialog.dismiss();
        }
        downloadProgressDialog = null;
    }

    private void showModelDownloadedDialog(String languageCode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.model_downloaded_dialog_title)
                .setMessage(getString(R.string.model_downloaded_dialog_message, languageCode))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void recreateTranslator() {
        translator.close();
        translator = new MlKitMarkdownTranslator(
                new MarkdownTranslationOptions.Builder()
                        .setEnableRegexFallbackProtection(isFallbackModeEnabled)
                        .build()
        );
        refreshDownloadedModelsAndButtonState();
    }

    private void refreshDownloadedModelsAndButtonState() {
        translator.getDownloadedLanguagePacks(new io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback() {
            @Override
            public void onSuccess(java.util.List<String> languageCodes) {
                runOnUiThread(() -> {
                    downloadedTargetModels.clear();
                    downloadedTargetModels.addAll(languageCodes);
                    updateDownloadButtonState();
                });
            }

            @Override
            public void onFailure(Exception error) {
                runOnUiThread(() -> updateDownloadButtonState());
            }
        });
    }

    private void translateMarkdown() {
        String markdown = originalMarkdownInput.getText().toString();
        setBusy(true);
        statusText.setText("Status: translating " + sourceLanguage() + " → " + targetLanguage() + "...");

        translator.translateMarkdown(
                markdown,
                sourceLanguage(),
                targetLanguage(),
                new io.github.godsarmy.mlmarkdown.api.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        runOnUiThread(() -> {
                            translatedMarkdownRaw.setText(translatedText);
                            if (isRenderMode) {
                                markwon.setMarkdown(translatedMarkdownRendered, translatedText);
                            }
                            statusText.setText("Status: translation complete");
                            setBusy(false);
                        });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(() -> {
                            statusText.setText("Status: translation failed - " + error.getMessage());
                            setBusy(false);
                        });
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        dismissDownloadProgressDialog();
        super.onDestroy();
        translator.close();
    }
}
