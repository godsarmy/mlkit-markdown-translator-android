package io.github.godsarmy.mlmarkdown.sample;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Spinner markdownSampleSpinner;
    private ImageButton translationErrorButton;
    private View translationProgressContainer;
    private TextView translationResultText;
    private Button downloadModelButton;
    private Button translateButton;

    private boolean isBusy;
    private boolean isTranslating;
    private boolean isRenderMode;
    private boolean isFallbackModeEnabled = true;
    private int activeDownloadRequestId;
    private AlertDialog downloadProgressDialog;
    private String latestTranslationError;
    @Nullable private TranslationTimingReport latestTimingReport;
    private final Set<String> downloadedTargetModels = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translator = createTranslator();
        markwon = Markwon.builder(this).usePlugin(TablePlugin.create(this)).build();

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
        markdownSampleSpinner = findViewById(R.id.markdownSampleSpinner);
        translationErrorButton = findViewById(R.id.translationErrorButton);
        translationProgressContainer = findViewById(R.id.translationProgressContainer);
        translationResultText = findViewById(R.id.translationResultText);
        downloadModelButton = findViewById(R.id.downloadModelButton);
        translateButton = findViewById(R.id.translateButton);

        originalMarkdownRendered.setMovementMethod(new ScrollingMovementMethod());
        translatedMarkdownRendered.setMovementMethod(new ScrollingMovementMethod());

        enableNestedScrollWithinPage(originalMarkdownInput);
        enableNestedScrollWithinPage(translatedMarkdownRaw);
        enableNestedScrollWithinPage(originalMarkdownRendered);
        enableNestedScrollWithinPage(translatedMarkdownRendered);

        translationErrorButton.setOnClickListener(v -> showTranslationErrorDialog());
    }

    private static void enableNestedScrollWithinPage(View scrollableView) {
        scrollableView.setOnTouchListener(
                (v, event) -> {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                            || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return false;
                });
    }

    private void setupLanguageSpinners() {
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this, R.array.language_codes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(adapter);
        targetLanguageSpinner.setAdapter(adapter);

        sourceLanguageSpinner.setSelection(0); // en
        targetLanguageSpinner.setSelection(1); // es
    }

    private void setupActions() {
        downloadModelButton.setOnClickListener(v -> onModelButtonClicked());
        translateButton.setOnClickListener(v -> translateMarkdown());

        markdownSampleSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        loadSelectedMarkdownSample(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // no-op
                    }
                });

        targetLanguageSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        refreshDownloadedModelsAndButtonState();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        updateDownloadButtonState();
                    }
                });

        renderModeSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    isRenderMode = isChecked;
                    applyRenderMode();
                });

        fallbackModeSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    isFallbackModeEnabled = isChecked;
                    recreateTranslator();
                });

        originalMarkdownInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isRenderMode) {
                            markwon.setMarkdown(originalMarkdownRendered, s.toString());
                        }
                    }
                });

        refreshDownloadedModelsAndButtonState();
        loadSelectedMarkdownSample(markdownSampleSpinner.getSelectedItemPosition());
        applyRenderMode();
    }

    private void loadSelectedMarkdownSample(int position) {
        String markdown = markdownSampleAt(position).load(this);
        originalMarkdownInput.setText(markdown);
        translatedMarkdownRaw.setText("");
        translatedMarkdownRendered.setText("");
        if (isRenderMode) {
            markwon.setMarkdown(originalMarkdownRendered, markdown);
        }
    }

    private static MarkdownSample markdownSampleAt(int position) {
        List<MarkdownSample> samples =
                List.of(
                        new MarkdownSample("markdown/simple.md"),
                        new MarkdownSample("markdown/large-prose.md"),
                        new MarkdownSample("markdown/complex-structure.md"),
                        new MarkdownSample("markdown/mixed-worst-case.md"),
                        new MarkdownSample("markdown/huge-document.md"));
        if (position < 0 || position >= samples.size()) {
            return samples.get(0);
        }
        return samples.get(position);
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
        updateDownloadButtonState();
        updateTranslateButtonState();
        updateTranslationProgressState();
    }

    private void updateTranslationProgressState() {
        translationProgressContainer.setVisibility(isTranslating ? View.VISIBLE : View.GONE);
    }

    private void updateDownloadButtonState() {
        boolean downloaded = downloadedTargetModels.contains(targetLanguage());
        downloadModelButton.setEnabled(!isBusy);
        downloadModelButton.setText(downloaded ? R.string.delete_model : R.string.download_model);
    }

    private void updateTranslateButtonState() {
        boolean downloaded = downloadedTargetModels.contains(targetLanguage());
        translateButton.setEnabled(!isBusy && downloaded);
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
                .setPositiveButton(
                        R.string.delete_model, (dialog, which) -> deleteTargetModel(languageCode))
                .show();
    }

    private void deleteTargetModel(String languageCode) {
        setBusy(true);

        translator.deleteLanguagePack(
                languageCode,
                new io.github.godsarmy.mlmarkdown.api.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(
                                () -> {
                                    downloadedTargetModels.remove(languageCode);
                                    setBusy(false);
                                });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(() -> setBusy(false));
                    }
                });
    }

    private void downloadTargetModel(String languageCode) {
        if (downloadedTargetModels.contains(languageCode)) {
            updateDownloadButtonState();
            return;
        }

        setBusy(true);

        int requestId = ++activeDownloadRequestId;
        showDownloadProgressDialog(languageCode, requestId);

        translator.ensureLanguageModelDownloaded(
                languageCode,
                new io.github.godsarmy.mlmarkdown.api.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(
                                () -> {
                                    if (requestId != activeDownloadRequestId) {
                                        return;
                                    }

                                    activeDownloadRequestId = 0;
                                    dismissDownloadProgressDialog();
                                    downloadedTargetModels.add(languageCode);
                                    setBusy(false);
                                });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(
                                () -> {
                                    if (requestId != activeDownloadRequestId) {
                                        return;
                                    }

                                    activeDownloadRequestId = 0;
                                    dismissDownloadProgressDialog();
                                    setBusy(false);
                                });
                    }
                });
    }

    private void showDownloadProgressDialog(String languageCode, int requestId) {
        dismissDownloadProgressDialog();
        downloadProgressDialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.model_download_progress_title)
                        .setMessage(
                                getString(R.string.model_download_progress_message, languageCode))
                        .setCancelable(true)
                        .setNegativeButton(
                                R.string.cancel_download,
                                (dialog, which) -> cancelActiveDownload(requestId))
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
        setBusy(false);
    }

    private void dismissDownloadProgressDialog() {
        if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
            downloadProgressDialog.dismiss();
        }
        downloadProgressDialog = null;
    }

    private void recreateTranslator() {
        translator.close();
        translator = createTranslator();
        refreshDownloadedModelsAndButtonState();
    }

    private MlKitMarkdownTranslator createTranslator() {
        return new MlKitMarkdownTranslator(
                new MarkdownTranslationOptions.Builder()
                        .setEnableRegexFallbackProtection(isFallbackModeEnabled)
                        .setTranslationTimingListener(report -> latestTimingReport = report)
                        .build());
    }

    private void refreshDownloadedModelsAndButtonState() {
        translator.getDownloadedLanguagePacks(
                new io.github.godsarmy.mlmarkdown.api.LanguagePacksCallback() {
                    @Override
                    public void onSuccess(java.util.List<String> languageCodes) {
                        runOnUiThread(
                                () -> {
                                    downloadedTargetModels.clear();
                                    downloadedTargetModels.addAll(languageCodes);
                                    updateDownloadButtonState();
                                    updateTranslateButtonState();
                                });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(
                                () -> {
                                    updateDownloadButtonState();
                                    updateTranslateButtonState();
                                });
                    }
                });
    }

    private void translateMarkdown() {
        String markdown = originalMarkdownInput.getText().toString();
        int rawCharCount = markdown.length();

        isTranslating = true;
        latestTimingReport = null;
        setBusy(true);
        clearTranslationError();
        clearTranslationResult();

        translator.translateMarkdown(
                markdown,
                sourceLanguage(),
                targetLanguage(),
                new io.github.godsarmy.mlmarkdown.api.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        runOnUiThread(
                                () -> {
                                    translatedMarkdownRaw.setText(translatedText);
                                    if (isRenderMode) {
                                        markwon.setMarkdown(
                                                translatedMarkdownRendered, translatedText);
                                    }

                                    TranslationTimingReport report = latestTimingReport;
                                    long durationMs =
                                            report != null ? report.getTotalDurationMs() : 0L;
                                    int tokenCount =
                                            report != null ? report.getTotalTokenCount() : 0;
                                    int chunkCount =
                                            report != null ? report.getTotalChunkCount() : 0;
                                    translationResultText.setText(
                                            getString(
                                                    R.string.translation_result_success,
                                                    rawCharCount,
                                                    durationMs,
                                                    tokenCount,
                                                    chunkCount));
                                    translationResultText.setVisibility(View.VISIBLE);

                                    clearTranslationError();
                                    isTranslating = false;
                                    setBusy(false);
                                });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(
                                () -> {
                                    String reason = error.getMessage();
                                    if (reason == null || reason.isBlank()) {
                                        reason = "Unknown error";
                                    }

                                    showTranslationError(reason);
                                    translationResultText.setText(
                                            getString(R.string.translation_result_failure, reason));
                                    translationResultText.setVisibility(View.VISIBLE);

                                    isTranslating = false;
                                    setBusy(false);
                                });
                    }
                });
    }

    private void clearTranslationError() {
        latestTranslationError = null;
        translationErrorButton.setVisibility(View.GONE);
    }

    private void clearTranslationResult() {
        translationResultText.setText("");
        translationResultText.setVisibility(View.GONE);
    }

    private void showTranslationError(String details) {
        latestTranslationError = details == null ? "Unknown error" : details;
        translationErrorButton.setVisibility(View.VISIBLE);
    }

    private void showTranslationErrorDialog() {
        if (latestTranslationError == null || latestTranslationError.isBlank()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.translation_error_dialog_title)
                .setMessage(latestTranslationError)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        dismissDownloadProgressDialog();
        super.onDestroy();
        translator.close();
    }

    private static final class MarkdownSample {
        @Nullable private final String assetPath;

        private MarkdownSample(@Nullable String assetPath) {
            this.assetPath = assetPath;
        }

        private String load(MainActivity activity) {
            if (assetPath == null) {
                return activity.getString(R.string.default_markdown);
            }

            try (java.io.InputStream inputStream = activity.getAssets().open(assetPath)) {
                return new String(
                        inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                return activity.getString(R.string.default_markdown);
            }
        }
    }
}
