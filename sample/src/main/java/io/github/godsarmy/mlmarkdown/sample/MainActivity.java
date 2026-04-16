package io.github.godsarmy.mlmarkdown.sample;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MainActivity extends AppCompatActivity {
    private enum SourceEntryType {
        SAMPLE,
        LOAD_URL
    }

    private MlKitMarkdownTranslator translator;
    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
    private final DownloadConditions downloadConditions = new DownloadConditions.Builder().build();

    private EditText originalMarkdownInput;
    private EditText translatedMarkdownRaw;
    private WebView inputRenderedHtml;
    private WebView outputRenderedHtml;
    private SwitchMaterial renderModeToggle;
    private ImageButton leftMenuButton;
    private DrawerLayout mainDrawerLayout;
    private NavigationView leftNavigationView;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;
    private AppCompatAutoCompleteTextView sampleAssetInput;
    private View exampleSourceContainer;
    private ImageButton translationErrorButton;
    private View translationProgressContainer;
    private TextView translationResultText;
    private MaterialButton downloadModelButton;
    private Button translateButton;
    private Button explainButton;

    private boolean isBusy;
    private boolean isTranslating;
    private boolean isRenderMode;
    private boolean preserveNewlines = true;
    private boolean preserveListPrefixes = true;
    private boolean preserveBlockquotes = true;
    private boolean normalizeCustomBlockTags = true;
    private boolean protectAutolinks = true;
    private boolean enableRegexFallbackProtection = true;
    private boolean preserveWhitespaceAroundProtectedSegments = true;
    private String tokenMarker = MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER;
    private int maxCharsPerChunk = MarkdownTranslationOptions.DEFAULT_MAX_CHARS_PER_CHUNK;
    private int activeDownloadRequestId;
    private AlertDialog downloadProgressDialog;
    private String latestTranslationError;
    @Nullable private TranslationTimingReport latestTimingReport;
    private final Set<String> downloadedTargetModels = new HashSet<>();
    private final ExecutorService sourceLoaderExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> translationOptionsLauncher;
    private boolean isSourceLoading;
    private final List<SourceSelectorEntry> sourceEntries = new ArrayList<>();
    private int selectedSourcePosition;
    @Nullable private KeyListener sampleAssetInputKeyListener;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translator = createTranslator();
        bindViews();
        registerTranslationOptionsLauncher();
        setupLanguageSpinners();
        setupActions();
    }

    private void bindViews() {
        originalMarkdownInput = findViewById(R.id.originalMarkdownInput);
        translatedMarkdownRaw = findViewById(R.id.translatedMarkdownRaw);
        inputRenderedHtml = findViewById(R.id.inputRenderedHtml);
        outputRenderedHtml = findViewById(R.id.outputRenderedHtml);
        renderModeToggle = findViewById(R.id.renderModeToggle);
        leftMenuButton = findViewById(R.id.leftMenuButton);
        mainDrawerLayout = findViewById(R.id.mainDrawerLayout);
        leftNavigationView = findViewById(R.id.leftNavigationView);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        sampleAssetInput = findViewById(R.id.sampleAssetInput);
        exampleSourceContainer = findViewById(R.id.exampleSourceContainer);
        translationErrorButton = findViewById(R.id.translationErrorButton);
        translationProgressContainer = findViewById(R.id.translationProgressContainer);
        translationResultText = findViewById(R.id.translationResultText);
        downloadModelButton = findViewById(R.id.downloadModelButton);
        translateButton = findViewById(R.id.translateButton);
        explainButton = findViewById(R.id.explainButton);

        setupWebView(inputRenderedHtml);
        setupWebView(outputRenderedHtml);

        enableNestedScrollWithinPage(originalMarkdownInput);
        enableNestedScrollWithinPage(translatedMarkdownRaw);
        enableNestedScrollWithinPage(inputRenderedHtml);
        enableNestedScrollWithinPage(outputRenderedHtml);

        translationErrorButton.setOnClickListener(v -> showTranslationErrorDialog());
        sampleAssetInputKeyListener = sampleAssetInput.getKeyListener();
        applyDrawerHeaderInsets();
    }

    private void applyDrawerHeaderInsets() {
        if (leftNavigationView.getHeaderCount() == 0) {
            return;
        }
        View header = leftNavigationView.getHeaderView(0);
        int initialTopPadding = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(
                header,
                (view, insets) -> {
                    int statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    view.setPadding(
                            view.getPaddingLeft(),
                            initialTopPadding + statusBarInset,
                            view.getPaddingRight(),
                            view.getPaddingBottom());
                    return insets;
                });
        ViewCompat.requestApplyInsets(header);
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

    private void setupWebView(WebView webView) {
        webView.setBackgroundColor(0x00000000);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
    }

    private void renderMarkdownToWebView(WebView webView, String markdown) {
        Node node = markdownParser.parse(markdown == null ? "" : markdown);
        String html = htmlRenderer.render(node);
        webView.loadDataWithBaseURL(null, wrapHtmlDocument(html), "text/html", "utf-8", null);
    }

    private String wrapHtmlDocument(String body) {
        return "<html><head><meta charset='utf-8' /><style>body{color:#1D1B20;font-family:sans-serif;padding:0;margin:0;}pre{white-space:pre-wrap;}code{white-space:pre-wrap;}</style></head><body>"
                + body
                + "</body></html>";
    }

    private boolean isValidHttpUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception ignored) {
            return false;
        }
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

    private void setupSourceSelector() {
        sourceEntries.clear();
        String[] sampleLabels = getResources().getStringArray(R.array.markdown_sample_options);
        for (int i = 0; i < sampleLabels.length; i++) {
            sourceEntries.add(
                    new SourceSelectorEntry(
                            sampleLabels[i],
                            R.drawable.ic_source_sample,
                            SourceEntryType.SAMPLE,
                            i));
        }
        sourceEntries.add(
                new SourceSelectorEntry(
                        getString(R.string.source_selector_load_url),
                        R.drawable.ic_source_url,
                        SourceEntryType.LOAD_URL,
                        -1));
        SourceSelectorAdapter adapter = new SourceSelectorAdapter();
        sampleAssetInput.setAdapter(adapter);
        sampleAssetInput.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedSourcePosition = position;
                    SourceSelectorEntry entry = sourceEntryAt(position);
                    if (entry.type == SourceEntryType.SAMPLE && entry.sampleIndex >= 0) {
                        loadSelectedMarkdownSample(entry.sampleIndex);
                    } else if (entry.type == SourceEntryType.LOAD_URL) {
                        sampleAssetInput.setText("", false);
                    }
                    updateSourceInputState();
                });
    }

    private SourceSelectorEntry sourceEntryAt(int position) {
        if (position < 0 || position >= sourceEntries.size()) {
            return sourceEntries.get(0);
        }
        return sourceEntries.get(position);
    }

    private boolean isLoadUrlSelected() {
        return sourceEntryAt(selectedSourcePosition).type == SourceEntryType.LOAD_URL;
    }

    private void setupActions() {
        downloadModelButton.setOnClickListener(v -> onModelButtonClicked());
        translateButton.setOnClickListener(v -> translateMarkdown());
        explainButton.setOnClickListener(v -> openExplainScreen());
        leftMenuButton.setOnClickListener(v -> mainDrawerLayout.openDrawer(GravityCompat.START));
        leftNavigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        updateVersionMenuItemTitle();

        setupSourceSelector();

        sampleAssetInput.setOnTouchListener(
                (v, event) -> {
                    if (event.getAction() != MotionEvent.ACTION_UP) {
                        return false;
                    }
                    if (isTapOnEndDrawable(sampleAssetInput, event)) {
                        sampleAssetInput.showDropDown();
                        return true;
                    }
                    if (isLoadUrlSelected() && isTapOnStartDrawable(sampleAssetInput, event)) {
                        loadMarkdownFromUrlInput();
                        return true;
                    }
                    return false;
                });

        sampleAssetInput.setOnClickListener(
                v -> {
                    if (!isLoadUrlSelected()) {
                        sampleAssetInput.showDropDown();
                    }
                });

        sampleAssetInput.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (!isLoadUrlSelected()) {
                        return false;
                    }
                    if (actionId == EditorInfo.IME_ACTION_GO
                            || actionId == EditorInfo.IME_ACTION_DONE
                            || actionId == EditorInfo.IME_ACTION_SEND
                            || actionId == EditorInfo.IME_NULL) {
                        loadMarkdownFromUrlInput();
                        return true;
                    }
                    return false;
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

        renderModeToggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    isRenderMode = isChecked;
                    applyRenderMode();
                });

        sampleAssetInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (isLoadUrlSelected()) {
                            sampleAssetInput.dismissDropDown();
                        }
                        updateSourceInputState();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
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
                            renderMarkdownToWebView(inputRenderedHtml, s.toString());
                        }
                        updateExplainButtonState();
                    }
                });

        refreshDownloadedModelsAndButtonState();
        selectedSourcePosition = 0;
        sampleAssetInput.setText(sourceEntryAt(selectedSourcePosition).label, false);
        loadSelectedMarkdownSample(0);
        applyRenderMode();
        updateExplainButtonState();
        updateSourceInputState();
    }

    private void loadSelectedMarkdownSample(int position) {
        String markdown = markdownSampleAt(position).load(this);
        originalMarkdownInput.setText(markdown);
        translatedMarkdownRaw.setText("");
        if (isRenderMode) {
            renderMarkdownToWebView(inputRenderedHtml, markdown);
            renderMarkdownToWebView(outputRenderedHtml, "");
        }
    }

    private static MarkdownSample markdownSampleAt(int position) {
        List<MarkdownSample> samples =
                List.of(
                        new MarkdownSample("markdown/simple.md"),
                        new MarkdownSample("markdown/large-prose.md"),
                        new MarkdownSample("markdown/complex-structure.md"),
                        new MarkdownSample("markdown/basic-syntax-edge-cases.md"),
                        new MarkdownSample("markdown/mixed-worst-case.md"),
                        new MarkdownSample("markdown/huge-document.md"),
                        new MarkdownSample("markdown/full-markdown.md"));
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
            inputRenderedHtml.setVisibility(View.VISIBLE);

            translatedMarkdownRaw.setVisibility(View.GONE);
            outputRenderedHtml.setVisibility(View.VISIBLE);

            renderMarkdownToWebView(inputRenderedHtml, original);
            renderMarkdownToWebView(outputRenderedHtml, translated);
            return;
        }

        originalMarkdownInput.setEnabled(true);
        originalMarkdownInput.setFocusable(true);
        originalMarkdownInput.setFocusableInTouchMode(true);
        originalMarkdownInput.setVisibility(View.VISIBLE);
        inputRenderedHtml.setVisibility(View.GONE);

        translatedMarkdownRaw.setVisibility(View.VISIBLE);
        outputRenderedHtml.setVisibility(View.GONE);
    }

    private void setBusy(boolean busy) {
        isBusy = busy;
        updateDownloadButtonState();
        updateTranslateButtonState();
        updateExplainButtonState();
        updateTranslationProgressState();
        updateSourceInputState();
    }

    private void setSourceLoading(boolean loading) {
        isSourceLoading = loading;
        updateSourceInputState();
    }

    private void updateSourceInputState() {
        boolean enabled = !isBusy && !isSourceLoading;
        boolean loadUrlSelected = isLoadUrlSelected();
        sampleAssetInput.setEnabled(enabled);
        sampleAssetInput.setFocusable(loadUrlSelected && enabled);
        sampleAssetInput.setFocusableInTouchMode(loadUrlSelected && enabled);
        sampleAssetInput.setCursorVisible(loadUrlSelected && enabled);
        sampleAssetInput.setKeyListener(loadUrlSelected ? sampleAssetInputKeyListener : null);
        sampleAssetInput.setThreshold(loadUrlSelected ? Integer.MAX_VALUE : 0);
        sampleAssetInput.setInputType(
                loadUrlSelected
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                        : InputType.TYPE_NULL);
        if (loadUrlSelected) {
            sampleAssetInput.setHint(R.string.source_url_hint);
            sampleAssetInput.setImeOptions(EditorInfo.IME_ACTION_GO);
            sampleAssetInput.setImeActionLabel(
                    getString(R.string.source_action_load), EditorInfo.IME_ACTION_GO);
        } else {
            sampleAssetInput.dismissDropDown();
            sampleAssetInput.setHint(null);
            sampleAssetInput.setImeActionLabel(null, EditorInfo.IME_ACTION_NONE);
            sampleAssetInput.setImeOptions(EditorInfo.IME_ACTION_NONE);
            String label = sourceEntryAt(selectedSourcePosition).label;
            if (!label.contentEquals(sampleAssetInput.getText())) {
                sampleAssetInput.setText(label, false);
            }
        }
        updateSourceSelectorDrawables(loadUrlSelected);
        exampleSourceContainer.setVisibility(View.VISIBLE);
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_advanced_parameters) {
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            translationOptionsLauncher.launch(
                    TranslationOptionsActivity.createIntent(
                            this, translationOptionsBuilder().build()));
            return true;
        }
        if (itemId == R.id.menu_help_feedback) {
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            openHelpAndFeedback();
            return true;
        }
        if (itemId == R.id.menu_version) {
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    private void updateVersionMenuItemTitle() {
        MenuItem versionItem = leftNavigationView.getMenu().findItem(R.id.menu_version);
        if (versionItem == null) {
            return;
        }
        versionItem.setTitle(getString(R.string.version_format, resolveVersionName()));
    }

    private String resolveVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (packageInfo.versionName != null && !packageInfo.versionName.isBlank()) {
                return packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return "unknown";
    }

    private void openHelpAndFeedback() {
        startActivity(HelpActivity.createIntent(this));
    }

    private void updateSourceSelectorDrawables(boolean loadUrlSelected) {
        SourceSelectorEntry entry = sourceEntryAt(selectedSourcePosition);
        int leftIcon = loadUrlSelected ? R.drawable.ic_source_url : entry.iconRes;
        int rightIcon = R.drawable.ic_source_dropdown;
        sampleAssetInput.setCompoundDrawablesRelativeWithIntrinsicBounds(leftIcon, 0, rightIcon, 0);
        sampleAssetInput.setCompoundDrawableTintList(
                ColorStateList.valueOf(getColor(R.color.mlkit_on_surface_variant)));
    }

    private static boolean isTapOnStartDrawable(TextView textView, MotionEvent event) {
        android.graphics.drawable.Drawable[] drawables = textView.getCompoundDrawablesRelative();
        android.graphics.drawable.Drawable startDrawable = drawables[0];
        if (startDrawable == null) {
            return false;
        }
        int drawableWidth = startDrawable.getBounds().width();
        int drawableEnd = textView.getPaddingStart() + drawableWidth;
        return event.getX() <= drawableEnd;
    }

    private static boolean isTapOnEndDrawable(TextView textView, MotionEvent event) {
        android.graphics.drawable.Drawable[] drawables = textView.getCompoundDrawablesRelative();
        android.graphics.drawable.Drawable endDrawable = drawables[2];
        if (endDrawable == null) {
            return false;
        }
        int drawableWidth = endDrawable.getBounds().width();
        int drawableStart = textView.getWidth() - textView.getPaddingEnd() - drawableWidth;
        return event.getX() >= drawableStart;
    }

    private void updateTranslationProgressState() {
        translationProgressContainer.setVisibility(isTranslating ? View.VISIBLE : View.GONE);
    }

    private void updateDownloadButtonState() {
        boolean downloaded = isTargetModelAvailable();
        boolean builtIn = isBuiltInLanguage(targetLanguage());
        downloadModelButton.setEnabled(!isBusy && !builtIn);
        downloadModelButton.setIconResource(
                downloaded ? R.drawable.ic_model_delete : R.drawable.ic_model_download);
        downloadModelButton.setContentDescription(
                getString(
                        downloaded
                                ? R.string.delete_model_icon_content_description
                                : R.string.download_model_icon_content_description));
    }

    private void updateTranslateButtonState() {
        translateButton.setEnabled(!isBusy && isTargetModelAvailable());
    }

    private void updateExplainButtonState() {
        String markdown = originalMarkdownInput.getText().toString();
        explainButton.setEnabled(!isBusy && !markdown.trim().isEmpty());
    }

    private void openExplainScreen() {
        String markdown = originalMarkdownInput.getText().toString();
        if (markdown.trim().isEmpty()) {
            return;
        }
        startActivity(
                ExplainMarkdownActivity.createIntent(
                        this, markdown, translationOptionsBuilder().build()));
    }

    private String sourceLanguage() {
        return String.valueOf(sourceLanguageSpinner.getSelectedItem());
    }

    private String targetLanguage() {
        return String.valueOf(targetLanguageSpinner.getSelectedItem());
    }

    private void onModelButtonClicked() {
        String language = targetLanguage();
        if (isBuiltInLanguage(language)) {
            return;
        }
        if (downloadedTargetModels.contains(normalizeLanguageCode(language))) {
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
        String normalizedLanguageCode = normalizeLanguageCode(languageCode);
        if (normalizedLanguageCode == null || isBuiltInLanguage(languageCode)) {
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
                                            downloadedTargetModels.remove(normalizedLanguageCode);
                                            setBusy(false);
                                        }))
                .addOnFailureListener(error -> runOnUiThread(() -> setBusy(false)));
    }

    private void downloadTargetModel(String languageCode) {
        String normalizedLanguageCode = normalizeLanguageCode(languageCode);
        if (normalizedLanguageCode == null || isBuiltInLanguage(languageCode)) {
            return;
        }

        if (downloadedTargetModels.contains(normalizedLanguageCode)) {
            updateDownloadButtonState();
            return;
        }

        setBusy(true);

        int requestId = ++activeDownloadRequestId;
        showDownloadProgressDialog(languageCode, requestId);

        TranslateRemoteModel model =
                new TranslateRemoteModel.Builder(normalizedLanguageCode).build();
        remoteModelManager
                .download(model, downloadConditions)
                .addOnSuccessListener(
                        unused ->
                                runOnUiThread(
                                        () -> {
                                            if (requestId != activeDownloadRequestId) {
                                                return;
                                            }

                                            activeDownloadRequestId = 0;
                                            dismissDownloadProgressDialog();
                                            downloadedTargetModels.add(normalizedLanguageCode);
                                            setBusy(false);
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            if (requestId != activeDownloadRequestId) {
                                                return;
                                            }

                                            activeDownloadRequestId = 0;
                                            dismissDownloadProgressDialog();
                                            setBusy(false);
                                        }));
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
                translationOptionsBuilder()
                        .setTranslationTimingListener(report -> latestTimingReport = report)
                        .build());
    }

    private MarkdownTranslationOptions.Builder translationOptionsBuilder() {
        return new MarkdownTranslationOptions.Builder()
                .setPreserveNewlines(preserveNewlines)
                .setPreserveListPrefixes(preserveListPrefixes)
                .setPreserveBlockquotes(preserveBlockquotes)
                .setNormalizeCustomBlockTags(normalizeCustomBlockTags)
                .setProtectAutolinks(protectAutolinks)
                .setEnableRegexFallbackProtection(enableRegexFallbackProtection)
                .setPreserveWhitespaceAroundProtectedSegments(
                        preserveWhitespaceAroundProtectedSegments)
                .setMaxCharsPerChunk(maxCharsPerChunk)
                .setTokenMarker(tokenMarker);
    }

    private void registerTranslationOptionsLauncher() {
        translationOptionsLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() != RESULT_OK) {
                                return;
                            }
                            Intent data = result.getData();
                            if (data == null) {
                                return;
                            }
                            applyTranslationOptions(
                                    TranslationOptionsActivity.extractOptions(data));
                            recreateTranslator();
                        });
    }

    private void loadMarkdownFromUrlInput() {
        String url = sampleAssetInput.getText().toString().trim();
        if (!isValidHttpUrl(url)) {
            Toast.makeText(this, R.string.source_mode_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        setSourceLoading(true);
        sourceLoaderExecutor.execute(
                () -> {
                    try {
                        String markdown = readUrlContent(url);
                        if (markdown.isBlank()) {
                            throw new IOException(getString(R.string.error_markdown_url_load));
                        }
                        runOnUiThread(() -> applyLoadedMarkdown(markdown));
                    } catch (IOException e) {
                        runOnUiThread(
                                () ->
                                        Toast.makeText(
                                                        this,
                                                        readErrorMessage(
                                                                e,
                                                                R.string.error_markdown_url_load),
                                                        Toast.LENGTH_SHORT)
                                                .show());
                    } finally {
                        runOnUiThread(() -> setSourceLoading(false));
                    }
                });
    }

    private void applyLoadedMarkdown(String markdown) {
        originalMarkdownInput.setText(markdown);
        translatedMarkdownRaw.setText("");
        if (isRenderMode) {
            renderMarkdownToWebView(inputRenderedHtml, markdown);
            renderMarkdownToWebView(outputRenderedHtml, "");
        }
        clearTranslationError();
        clearTranslationResult();
    }

    private String readUrlContent(String urlValue) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.connect();

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return readText(inputStream).trim();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readText(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String readErrorMessage(IOException error, int fallbackResId) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return getString(fallbackResId);
        }
        return message;
    }

    private void applyTranslationOptions(MarkdownTranslationOptions options) {
        preserveNewlines = options.preserveNewlines();
        preserveListPrefixes = options.preserveListPrefixes();
        preserveBlockquotes = options.preserveBlockquotes();
        normalizeCustomBlockTags = options.normalizeCustomBlockTags();
        protectAutolinks = options.protectAutolinks();
        enableRegexFallbackProtection = options.enableRegexFallbackProtection();
        preserveWhitespaceAroundProtectedSegments =
                options.preserveWhitespaceAroundProtectedSegments();
        tokenMarker = options.tokenMarker();
        maxCharsPerChunk = options.maxCharsPerChunk();
    }

    private void refreshDownloadedModelsAndButtonState() {
        remoteModelManager
                .getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(
                        models ->
                                runOnUiThread(
                                        () -> {
                                            downloadedTargetModels.clear();
                                            for (TranslateRemoteModel model : models) {
                                                downloadedTargetModels.add(model.getLanguage());
                                            }
                                            updateDownloadButtonState();
                                            updateTranslateButtonState();
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            updateDownloadButtonState();
                                            updateTranslateButtonState();
                                        }));
    }

    private boolean isTargetModelAvailable() {
        return isBuiltInLanguage(targetLanguage())
                || downloadedTargetModels.contains(normalizeLanguageCode(targetLanguage()));
    }

    private static boolean isBuiltInLanguage(String languageCode) {
        return TranslateLanguage.ENGLISH.equals(normalizeLanguageCode(languageCode));
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
                                        renderMarkdownToWebView(outputRenderedHtml, translatedText);
                                    }

                                    TranslationTimingReport report = latestTimingReport;
                                    long durationMs =
                                            report != null ? report.getTotalDurationMs() : 0L;
                                    int tokenCount =
                                            report != null ? report.getTotalTokenCount() : 0;
                                    int chunkCount =
                                            report != null ? report.getTotalChunkCount() : 0;
                                    int chunkRecoveryCount =
                                            report != null
                                                    ? report.getChunkParseRecoveryCount()
                                                    : 0;
                                    boolean regexFallbackTriggered =
                                            report != null && report.isRegexFallbackTriggered();
                                    translationResultText.setText(
                                            getString(
                                                    R.string.translation_result_success,
                                                    rawCharCount,
                                                    durationMs,
                                                    tokenCount,
                                                    chunkCount,
                                                    chunkRecoveryCount,
                                                    regexFallbackTriggered));
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
        sourceLoaderExecutor.shutdownNow();
        super.onDestroy();
        translator.close();
    }

    private static final class SourceSelectorEntry {
        private final String label;
        private final int iconRes;
        private final SourceEntryType type;
        private final int sampleIndex;

        private SourceSelectorEntry(
                String label, int iconRes, SourceEntryType type, int sampleIndex) {
            this.label = label;
            this.iconRes = iconRes;
            this.type = type;
            this.sampleIndex = sampleIndex;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final class SourceSelectorAdapter extends ArrayAdapter<SourceSelectorEntry>
            implements ListAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        private SourceSelectorAdapter() {
            super(MainActivity.this, R.layout.item_source_selector_selected, sourceEntries);
        }

        @Override
        public int getCount() {
            return sourceEntries.size();
        }

        @Override
        public SourceSelectorEntry getItem(int position) {
            return sourceEntryAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view =
                    convertView != null
                            ? convertView
                            : inflater.inflate(
                                    R.layout.item_source_selector_dropdown, parent, false);
            bindRow(view, sourceEntryAt(position), position == sourceEntries.size() - 1);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view =
                    convertView != null
                            ? convertView
                            : inflater.inflate(
                                    R.layout.item_source_selector_dropdown, parent, false);
            bindRow(view, sourceEntryAt(position), position == sourceEntries.size() - 1);
            return view;
        }

        private void bindRow(View row, SourceSelectorEntry entry, boolean showDivider) {
            ImageView icon = row.findViewById(R.id.sourceSelectorIcon);
            TextView text = row.findViewById(R.id.sourceSelectorText);
            icon.setImageResource(entry.iconRes);
            text.setText(entry.label);

            View divider = row.findViewById(R.id.sourceSelectorDivider);
            if (divider != null) {
                divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
            }
        }
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
