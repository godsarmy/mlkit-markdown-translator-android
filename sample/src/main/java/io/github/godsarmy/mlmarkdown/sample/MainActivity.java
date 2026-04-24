package io.github.godsarmy.mlmarkdown.sample;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_SOURCE_POSITION = "state_selected_source_position";
    private static final String STATE_RENDER_MODE = "state_render_mode";
    private static final String STATE_SOURCE_LANGUAGE = "state_source_language";
    private static final String STATE_TARGET_LANGUAGE = "state_target_language";

    private enum SourceEntryType {
        SAMPLE,
        LOAD_URL
    }

    private MlKitMarkdownTranslator translator;
    private final RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();

    private EditText originalMarkdownInput;
    private EditText translatedMarkdownRaw;
    private WebView inputRenderedHtml;
    private WebView outputRenderedHtml;
    private SwitchMaterial renderModeToggle;
    private ImageButton compareModeButton;
    private ImageButton saveTranslatedButton;
    private ImageButton shareTranslatedButton;
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
    private String latestTranslationError;
    @Nullable private TranslationTimingReport latestTimingReport;
    private final Set<String> downloadedTargetModels = new HashSet<>();
    private final List<String> downloadedLanguageOptions = new ArrayList<>();
    private final ExecutorService sourceLoaderExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> translationOptionsLauncher;
    private boolean isSourceLoading;
    private final List<SourceSelectorEntry> sourceEntries = new ArrayList<>();
    private int selectedSourcePosition;
    @Nullable private String pendingPreferredSourceLanguage;
    @Nullable private String pendingPreferredTargetLanguage;
    @Nullable private KeyListener sampleAssetInputKeyListener;
    private static final List<Extension> MARKDOWN_EXTENSIONS =
            Collections.singletonList(TablesExtension.create());
    private final Parser markdownParser = Parser.builder().extensions(MARKDOWN_EXTENSIONS).build();
    private final HtmlRenderer htmlRenderer =
            HtmlRenderer.builder().extensions(MARKDOWN_EXTENSIONS).build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            pendingPreferredSourceLanguage = savedInstanceState.getString(STATE_SOURCE_LANGUAGE);
            pendingPreferredTargetLanguage = savedInstanceState.getString(STATE_TARGET_LANGUAGE);
        }

        translator = createTranslator();
        bindViews();
        registerTranslationOptionsLauncher();
        setupLanguageSpinners();
        setupActions(savedInstanceState);
    }

    private void bindViews() {
        originalMarkdownInput = findViewById(R.id.originalMarkdownInput);
        translatedMarkdownRaw = findViewById(R.id.translatedMarkdownRaw);
        inputRenderedHtml = findViewById(R.id.inputRenderedHtml);
        outputRenderedHtml = findViewById(R.id.outputRenderedHtml);
        renderModeToggle = findViewById(R.id.renderModeToggle);
        compareModeButton = findViewById(R.id.compareModeButton);
        saveTranslatedButton = findViewById(R.id.saveTranslatedButton);
        shareTranslatedButton = findViewById(R.id.shareTranslatedButton);
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
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setScrollbarFadingEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
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
        String textColor = toCssColor(getColor(R.color.mlkit_on_background));
        String linkColor = toCssColor(getColor(R.color.mlkit_primary));
        String codeBackground = toCssColor(getColor(R.color.mlkit_code_block_bg));
        String codeText = toCssColor(getColor(R.color.mlkit_on_surface_variant));
        String tableBorder = toCssColor(getColor(R.color.mlkit_outline));
        String tableHeaderBackground = toCssColor(getColor(R.color.mlkit_surface));
        return "<html><head><meta charset='utf-8' /><meta name='color-scheme' content='light dark' /><style>"
                + "body{color:"
                + textColor
                + ";font-family:sans-serif;padding:0;margin:0;background:transparent;}"
                + "a{color:"
                + linkColor
                + ";}"
                + "pre,code{white-space:pre-wrap;background:"
                + codeBackground
                + ";color:"
                + codeText
                + ";border-radius:8px;}"
                + "code{padding:0.15em 0.35em;}"
                + "pre{padding:8px;}"
                + "table{border-collapse:collapse;width:100%;margin:8px 0;display:block;overflow-x:auto;}"
                + "th,td{border:1px solid "
                + tableBorder
                + ";padding:6px 8px;text-align:left;}"
                + "th{background:"
                + tableHeaderBackground
                + ";}"
                + "</style></head><body>"
                + body
                + "</body></html>";
    }

    private static String toCssColor(int colorInt) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & colorInt);
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
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, downloadedLanguageOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(adapter);
        targetLanguageSpinner.setAdapter(adapter);
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

    private void setupActions(@Nullable Bundle savedInstanceState) {
        translateButton.setOnClickListener(v -> translateMarkdown());
        explainButton.setOnClickListener(v -> openExplainScreen());
        compareModeButton.setOnClickListener(v -> openSideBySideCompare());
        saveTranslatedButton.setOnClickListener(v -> saveTranslatedMarkdown());
        shareTranslatedButton.setOnClickListener(v -> shareTranslatedMarkdown());
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
                    public void onNothingSelected(AdapterView<?> parent) {}
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

        translatedMarkdownRaw.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        updateShareButtonState();
                    }
                });

        initializeState(savedInstanceState);
        updateExplainButtonState();
        updateShareButtonState();
        updateSourceInputState();
        refreshDownloadedModelsAndButtonState();
    }

    private void initializeState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            selectedSourcePosition = 0;
            isRenderMode = false;
            renderModeToggle.setChecked(false);
            sampleAssetInput.setText(sourceEntryAt(selectedSourcePosition).label, false);
            loadSelectedMarkdownSample(0);
            applyRenderMode();
            return;
        }

        int restoredSourcePosition =
                savedInstanceState.getInt(STATE_SELECTED_SOURCE_POSITION, selectedSourcePosition);
        selectedSourcePosition =
                Math.max(0, Math.min(restoredSourcePosition, sourceEntries.size() - 1));

        isRenderMode =
                savedInstanceState.getBoolean(STATE_RENDER_MODE, renderModeToggle.isChecked());
        renderModeToggle.setChecked(isRenderMode);
        applyRenderMode();
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
        if (itemId == R.id.menu_manage_models) {
            mainDrawerLayout.closeDrawer(GravityCompat.START);
            startActivity(ModelManagementActivity.createIntent(this));
            return true;
        }
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
        translationProgressContainer.setVisibility(isTranslating ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateTranslateButtonState() {
        boolean hasDownloadedLanguages = !downloadedLanguageOptions.isEmpty();
        sourceLanguageSpinner.setEnabled(!isBusy && hasDownloadedLanguages);
        targetLanguageSpinner.setEnabled(!isBusy && hasDownloadedLanguages);
        translateButton.setEnabled(!isBusy && hasDownloadedLanguages && isTargetModelAvailable());

        if (!hasDownloadedLanguages) {
            translationResultText.setText(R.string.no_downloaded_models_message);
            translationResultText.setVisibility(View.VISIBLE);
        }
    }

    private void updateExplainButtonState() {
        String markdown = originalMarkdownInput.getText().toString();
        explainButton.setEnabled(!isBusy && !markdown.trim().isEmpty());
    }

    private void updateShareButtonState() {
        boolean hasTranslatedMarkdown =
                !translatedMarkdownRaw.getText().toString().trim().isEmpty();
        shareTranslatedButton.setEnabled(hasTranslatedMarkdown);
        saveTranslatedButton.setEnabled(hasTranslatedMarkdown);
        int tintColor =
                hasTranslatedMarkdown
                        ? getColor(R.color.mlkit_on_surface_variant)
                        : getColor(R.color.mlkit_outline);
        shareTranslatedButton.setImageTintList(ColorStateList.valueOf(tintColor));
        saveTranslatedButton.setImageTintList(ColorStateList.valueOf(tintColor));
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

    private void openSideBySideCompare() {
        startActivity(
                SideBySideCompareActivity.createIntent(
                        this,
                        originalMarkdownInput.getText().toString(),
                        translatedMarkdownRaw.getText().toString()));
    }

    private void shareTranslatedMarkdown() {
        String translatedMarkdown = translatedMarkdownRaw.getText().toString();
        if (translatedMarkdown.trim().isEmpty()) {
            Toast.makeText(this, R.string.share_translated_markdown_empty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, translatedMarkdown);
        startActivity(
                Intent.createChooser(
                        shareIntent, getString(R.string.share_translated_markdown_chooser_title)));
    }

    private void saveTranslatedMarkdown() {
        String translatedMarkdown = translatedMarkdownRaw.getText().toString();
        if (translatedMarkdown.trim().isEmpty()) {
            Toast.makeText(this, R.string.save_translated_markdown_empty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, R.string.save_translated_markdown_failure, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        String baseName = buildOutputBaseName();
        String nextFilename = nextAvailableDownloadFilename(baseName);
        if (nextFilename == null) {
            Toast.makeText(this, R.string.save_translated_markdown_failure, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Uri downloadsUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, nextFilename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");

        Uri itemUri = null;
        try {
            itemUri = getContentResolver().insert(downloadsUri, values);
            if (itemUri == null) {
                throw new IOException("Insert failed");
            }
            try (OutputStream outputStream = getContentResolver().openOutputStream(itemUri, "w")) {
                if (outputStream == null) {
                    throw new IOException("Open output stream failed");
                }
                outputStream.write(translatedMarkdown.getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(
                            this,
                            getString(R.string.save_translated_markdown_success, nextFilename),
                            Toast.LENGTH_SHORT)
                    .show();
        } catch (Exception e) {
            if (itemUri != null) {
                getContentResolver().delete(itemUri, null, null);
            }
            Toast.makeText(this, R.string.save_translated_markdown_failure, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String buildOutputBaseName() {
        String derived = deriveSourceName();
        String normalizedTarget = normalizeLanguageCode(targetLanguage());
        String targetCode =
                normalizedTarget == null || normalizedTarget.isBlank() ? "xx" : normalizedTarget;
        return sanitizeFilename(derived) + "-" + sanitizeFilename(targetCode);
    }

    private String deriveSourceName() {
        if (isLoadUrlSelected()) {
            String urlValue = sampleAssetInput.getText().toString().trim();
            try {
                Uri uri = Uri.parse(urlValue);
                String segment = uri.getLastPathSegment();
                if (segment != null && !segment.isBlank()) {
                    return stripExtension(segment);
                }
            } catch (Exception ignored) {
            }
            return "translated";
        }

        SourceSelectorEntry entry = sourceEntryAt(selectedSourcePosition);
        if (entry.type == SourceEntryType.SAMPLE && entry.label != null && !entry.label.isBlank()) {
            return stripExtension(entry.label.trim());
        }
        return "translated";
    }

    private static String stripExtension(String value) {
        int extensionIndex = value.lastIndexOf('.');
        if (extensionIndex > 0) {
            return value.substring(0, extensionIndex);
        }
        return value;
    }

    private static String sanitizeFilename(String value) {
        String sanitized = value == null ? "" : value.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.replaceAll("\\s+", "_");
        if (sanitized.isBlank()) {
            return "translated";
        }
        return sanitized;
    }

    @Nullable
    @RequiresApi(Build.VERSION_CODES.Q)
    private String nextAvailableDownloadFilename(String baseName) {
        String safeBase = sanitizeFilename(baseName);
        for (int suffix = 0; suffix < 10000; suffix++) {
            String candidate = suffix == 0 ? safeBase + ".md" : safeBase + "." + suffix + ".md";
            if (!downloadNameExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private boolean downloadNameExists(String displayName) {
        String[] projection = {MediaStore.MediaColumns._ID};
        String selection =
                MediaStore.MediaColumns.DISPLAY_NAME
                        + "=? AND "
                        + MediaStore.MediaColumns.RELATIVE_PATH
                        + "=?";
        String[] args = {displayName, "Download/"};
        try (Cursor cursor =
                getContentResolver()
                        .query(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                projection,
                                selection,
                                args,
                                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    private String sourceLanguage() {
        Object selected = sourceLanguageSpinner.getSelectedItem();
        return selected == null ? "" : String.valueOf(selected);
    }

    private String targetLanguage() {
        Object selected = targetLanguageSpinner.getSelectedItem();
        return selected == null ? "" : String.valueOf(selected);
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
                                            updateDownloadedLanguageOptions();
                                            updateTranslateButtonState();
                                        }))
                .addOnFailureListener(
                        error ->
                                runOnUiThread(
                                        () -> {
                                            downloadedTargetModels.clear();
                                            updateDownloadedLanguageOptions();
                                            updateTranslateButtonState();
                                        }));
    }

    private void updateDownloadedLanguageOptions() {
        String previousSource = sourceLanguage();
        String previousTarget = targetLanguage();
        downloadedLanguageOptions.clear();

        String[] supportedLanguages = getResources().getStringArray(R.array.language_codes);
        for (String language : supportedLanguages) {
            String normalized = normalizeLanguageCode(language);
            if (normalized != null && downloadedTargetModels.contains(normalized)) {
                downloadedLanguageOptions.add(language);
            }
        }
        Collections.sort(downloadedLanguageOptions);

        @SuppressWarnings("unchecked")
        ArrayAdapter<String> sourceAdapter =
                (ArrayAdapter<String>) sourceLanguageSpinner.getAdapter();
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> targetAdapter =
                (ArrayAdapter<String>) targetLanguageSpinner.getAdapter();
        if (sourceAdapter != null) {
            sourceAdapter.notifyDataSetChanged();
        }
        if (targetAdapter != null) {
            targetAdapter.notifyDataSetChanged();
        }

        String preferredSource =
                pendingPreferredSourceLanguage != null
                        ? pendingPreferredSourceLanguage
                        : previousSource;
        String preferredTarget =
                pendingPreferredTargetLanguage != null
                        ? pendingPreferredTargetLanguage
                        : previousTarget;

        restoreSpinnerSelection(sourceLanguageSpinner, preferredSource);
        restoreSpinnerSelection(targetLanguageSpinner, preferredTarget);

        pendingPreferredSourceLanguage = null;
        pendingPreferredTargetLanguage = null;
    }

    private static void restoreSpinnerSelection(Spinner spinner, String preferredLanguage) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        if (adapter == null || adapter.getCount() == 0) {
            return;
        }
        int preferredIndex = -1;
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (preferredLanguage.equals(item)) {
                preferredIndex = i;
                break;
            }
        }
        spinner.setSelection(preferredIndex >= 0 ? preferredIndex : 0);
    }

    private boolean isTargetModelAvailable() {
        String normalizedTargetLanguage = normalizeLanguageCode(targetLanguage());
        return normalizedTargetLanguage != null
                && downloadedTargetModels.contains(normalizedTargetLanguage);
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
        translationResultText.setVisibility(View.INVISIBLE);
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
        sourceLoaderExecutor.shutdownNow();
        super.onDestroy();
        translator.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_SOURCE_POSITION, selectedSourcePosition);
        outState.putBoolean(STATE_RENDER_MODE, isRenderMode);
        outState.putString(STATE_SOURCE_LANGUAGE, sourceLanguage());
        outState.putString(STATE_TARGET_LANGUAGE, targetLanguage());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (isRenderMode) {
            applyRenderMode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDownloadedModelsAndButtonState();
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
                return readText(inputStream);
            } catch (java.io.IOException e) {
                return activity.getString(R.string.default_markdown);
            }
        }
    }
}
