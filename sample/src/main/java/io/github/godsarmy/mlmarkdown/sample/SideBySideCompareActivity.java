package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class SideBySideCompareActivity extends AppCompatActivity {
    private static final String EXTRA_SOURCE_MARKDOWN = "extra_source_markdown";
    private static final String EXTRA_TRANSLATED_MARKDOWN = "extra_translated_markdown";
    private static final long TOGGLE_AUTO_HIDE_DELAY_MS = 2400L;
    private static final long TOGGLE_FADE_DURATION_MS = 180L;
    private TextView sourceText;
    private TextView translatedText;
    private WebView sourceRenderedHtml;
    private WebView translatedRenderedHtml;
    private ImageButton renderToggleButton;
    private ImageButton lineNumbersToggleButton;
    private ImageButton closeButton;
    private LineNumberGutterView sourceLineNumbers;
    private LineNumberGutterView translatedLineNumbers;
    private View sourceLineDivider;
    private View translatedLineDivider;
    private boolean syncingScroll;
    private boolean renderModeEnabled;
    private boolean lineNumbersEnabled;
    private boolean isRenderToggleVisible;
    private final Runnable hideRenderToggleRunnable = this::hideRenderToggle;
    private static final List<Extension> MARKDOWN_EXTENSIONS =
            Collections.singletonList(TablesExtension.create());
    private final Parser markdownParser = Parser.builder().extensions(MARKDOWN_EXTENSIONS).build();
    private final HtmlRenderer htmlRenderer =
            HtmlRenderer.builder().extensions(MARKDOWN_EXTENSIONS).build();

    public static Intent createIntent(
            Context context, String sourceMarkdown, String translatedMarkdown) {
        Intent intent = new Intent(context, SideBySideCompareActivity.class);
        intent.putExtra(EXTRA_SOURCE_MARKDOWN, sourceMarkdown);
        intent.putExtra(EXTRA_TRANSLATED_MARKDOWN, translatedMarkdown);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_side_by_side_compare);
        setTitle(R.string.compare_screen_title);

        hideStatusBar();

        View compareRoot = findViewById(R.id.compareRoot);
        sourceText = findViewById(R.id.compareSourceText);
        translatedText = findViewById(R.id.compareTranslatedText);
        sourceRenderedHtml = findViewById(R.id.compareSourceRenderedHtml);
        translatedRenderedHtml = findViewById(R.id.compareTranslatedRenderedHtml);
        renderToggleButton = findViewById(R.id.compareRenderToggleButton);
        lineNumbersToggleButton = findViewById(R.id.compareLineNumbersToggleButton);
        closeButton = findViewById(R.id.compareCloseButton);
        sourceLineNumbers = findViewById(R.id.compareSourceLineNumbers);
        translatedLineNumbers = findViewById(R.id.compareTranslatedLineNumbers);
        sourceLineDivider = findViewById(R.id.compareSourceLineDivider);
        translatedLineDivider = findViewById(R.id.compareTranslatedLineDivider);

        setupWebView(sourceRenderedHtml);
        setupWebView(translatedRenderedHtml);
        setupRawCompareText(sourceText);
        setupRawCompareText(translatedText);
        matchLineNumberStyle(sourceText, sourceLineNumbers);
        matchLineNumberStyle(translatedText, translatedLineNumbers);

        applySafeInsets(compareRoot);

        Intent intent = getIntent();
        String sourceMarkdown = intent.getStringExtra(EXTRA_SOURCE_MARKDOWN);
        String translatedMarkdown = intent.getStringExtra(EXTRA_TRANSLATED_MARKDOWN);
        sourceText.setText(sourceMarkdown == null ? "" : sourceMarkdown);
        translatedText.setText(translatedMarkdown == null ? "" : translatedMarkdown);
        sourceLineNumbers.bindTo(sourceText);
        translatedLineNumbers.bindTo(translatedText);
        sourceText.post(
                () -> {
                    invalidateLineNumberGutters();
                });

        sourceText.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    syncRawScrollByLine(sourceText, translatedText, scrollX, scrollY);
                    invalidateLineNumberGutters();
                });
        translatedText.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    syncRawScrollByLine(translatedText, sourceText, scrollX, scrollY);
                    invalidateLineNumberGutters();
                });
        sourceRenderedHtml.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        syncScroll(sourceRenderedHtml, translatedRenderedHtml, scrollX, scrollY));
        translatedRenderedHtml.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        syncScroll(translatedRenderedHtml, sourceRenderedHtml, scrollX, scrollY));

        renderToggleButton.setOnClickListener(v -> toggleRenderMode());
        lineNumbersToggleButton.setOnClickListener(v -> toggleLineNumbers());
        closeButton.setOnClickListener(v -> finish());
        applyRenderMode();
        showRenderToggleTemporarily();
    }

    private void setupWebView(WebView webView) {
        webView.setBackgroundColor(0x00000000);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
        webView.setScrollbarFadingEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
    }

    private static void setupRawCompareText(TextView textView) {
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setHorizontallyScrolling(true);
        textView.setHorizontalScrollBarEnabled(true);
        textView.setVerticalScrollBarEnabled(true);
    }

    private static void matchLineNumberStyle(
            TextView contentView, LineNumberGutterView lineNumbersView) {
        if (contentView == null || lineNumbersView == null) {
            return;
        }
        lineNumbersView.applyTextMetricsFrom(contentView);
    }

    private void toggleRenderMode() {
        renderModeEnabled = !renderModeEnabled;
        applyRenderMode();
        showRenderToggleTemporarily();
    }

    private void applyRenderMode() {
        if (renderModeEnabled) {
            sourceText.setVisibility(View.GONE);
            translatedText.setVisibility(View.GONE);
            sourceRenderedHtml.setVisibility(View.VISIBLE);
            translatedRenderedHtml.setVisibility(View.VISIBLE);
            if (lineNumbersToggleButton != null) {
                lineNumbersToggleButton.setEnabled(false);
                lineNumbersToggleButton.setClickable(false);
            }
            sourceLineNumbers.setVisibility(View.GONE);
            translatedLineNumbers.setVisibility(View.GONE);
            sourceLineDivider.setVisibility(View.GONE);
            translatedLineDivider.setVisibility(View.GONE);
            renderMarkdownToWebView(sourceRenderedHtml, sourceText.getText().toString());
            renderMarkdownToWebView(translatedRenderedHtml, translatedText.getText().toString());
        } else {
            sourceText.setVisibility(View.VISIBLE);
            translatedText.setVisibility(View.VISIBLE);
            sourceRenderedHtml.setVisibility(View.GONE);
            translatedRenderedHtml.setVisibility(View.GONE);
            if (lineNumbersToggleButton != null) {
                lineNumbersToggleButton.setEnabled(true);
                lineNumbersToggleButton.setClickable(true);
            }
            updateLineNumbersVisibility();
        }
        updateRenderToggleIcon();
        updateLineNumbersToggleIcon();
    }

    private void updateRenderToggleIcon() {
        renderToggleButton.setImageResource(
                renderModeEnabled
                        ? R.drawable.ic_render_preview_on
                        : R.drawable.ic_render_preview_off);
        int tintColor =
                renderModeEnabled
                        ? getColor(R.color.mlkit_primary)
                        : getColor(R.color.mlkit_on_surface_variant);
        renderToggleButton.setImageTintList(ColorStateList.valueOf(tintColor));
        renderToggleButton.setContentDescription(
                getString(
                        renderModeEnabled
                                ? R.string.compare_render_markdown_disable
                                : R.string.compare_render_markdown_enable));
    }

    private void showRenderToggleTemporarily() {
        if (renderToggleButton == null || lineNumbersToggleButton == null || closeButton == null) {
            return;
        }
        renderToggleButton.removeCallbacks(hideRenderToggleRunnable);
        if (!isRenderToggleVisible) {
            isRenderToggleVisible = true;
            renderToggleButton.setVisibility(View.VISIBLE);
            renderToggleButton.setClickable(true);
            lineNumbersToggleButton.setVisibility(View.VISIBLE);
            lineNumbersToggleButton.setClickable(lineNumbersToggleButton.isEnabled());
            closeButton.setVisibility(View.VISIBLE);
            closeButton.setClickable(true);
            renderToggleButton.animate().cancel();
            lineNumbersToggleButton.animate().cancel();
            closeButton.animate().cancel();
            renderToggleButton.setAlpha(0f);
            lineNumbersToggleButton.setAlpha(0f);
            closeButton.setAlpha(0f);
            renderToggleButton.animate().alpha(1f).setDuration(TOGGLE_FADE_DURATION_MS).start();
            lineNumbersToggleButton
                    .animate()
                    .alpha(lineNumbersToggleButton.isEnabled() ? 1f : 0.4f)
                    .setDuration(TOGGLE_FADE_DURATION_MS)
                    .start();
            closeButton.animate().alpha(1f).setDuration(TOGGLE_FADE_DURATION_MS).start();
        }
        renderToggleButton.postDelayed(hideRenderToggleRunnable, TOGGLE_AUTO_HIDE_DELAY_MS);
    }

    private void hideRenderToggle() {
        if (renderToggleButton == null
                || lineNumbersToggleButton == null
                || closeButton == null
                || !isRenderToggleVisible) {
            return;
        }
        renderToggleButton.removeCallbacks(hideRenderToggleRunnable);
        renderToggleButton.animate().cancel();
        lineNumbersToggleButton.animate().cancel();
        closeButton.animate().cancel();
        renderToggleButton
                .animate()
                .alpha(0f)
                .setDuration(TOGGLE_FADE_DURATION_MS)
                .withEndAction(
                        () -> {
                            renderToggleButton.setVisibility(View.INVISIBLE);
                            renderToggleButton.setClickable(false);
                            isRenderToggleVisible = false;
                        })
                .start();
        lineNumbersToggleButton
                .animate()
                .alpha(0f)
                .setDuration(TOGGLE_FADE_DURATION_MS)
                .withEndAction(
                        () -> {
                            lineNumbersToggleButton.setVisibility(View.INVISIBLE);
                            lineNumbersToggleButton.setClickable(false);
                        })
                .start();
        closeButton
                .animate()
                .alpha(0f)
                .setDuration(TOGGLE_FADE_DURATION_MS)
                .withEndAction(
                        () -> {
                            closeButton.setVisibility(View.INVISIBLE);
                            closeButton.setClickable(false);
                        })
                .start();
    }

    private void toggleLineNumbers() {
        if (renderModeEnabled) {
            return;
        }
        lineNumbersEnabled = !lineNumbersEnabled;
        updateLineNumbersVisibility();
        updateLineNumbersToggleIcon();
        showRenderToggleTemporarily();
    }

    private void updateLineNumbersToggleIcon() {
        if (lineNumbersToggleButton == null) {
            return;
        }
        lineNumbersToggleButton.setImageResource(R.drawable.ic_line_numbers);
        int tintColor =
                lineNumbersEnabled
                        ? getColor(R.color.mlkit_primary)
                        : getColor(R.color.mlkit_on_surface_variant);
        lineNumbersToggleButton.setImageTintList(ColorStateList.valueOf(tintColor));
        lineNumbersToggleButton.setAlpha(lineNumbersToggleButton.isEnabled() ? 1f : 0.4f);
        lineNumbersToggleButton.setContentDescription(
                getString(
                        lineNumbersEnabled
                                ? R.string.compare_line_numbers_disable
                                : R.string.compare_line_numbers_enable));
    }

    private void updateLineNumbersVisibility() {
        if (sourceLineNumbers == null || translatedLineNumbers == null) {
            return;
        }
        int visibility = lineNumbersEnabled ? View.VISIBLE : View.GONE;
        sourceLineNumbers.setVisibility(visibility);
        translatedLineNumbers.setVisibility(visibility);
        sourceLineDivider.setVisibility(visibility);
        translatedLineDivider.setVisibility(visibility);
        invalidateLineNumberGutters();
    }

    private void invalidateLineNumberGutters() {
        if (sourceLineNumbers != null && sourceLineNumbers.getVisibility() == View.VISIBLE) {
            sourceLineNumbers.invalidate();
        }
        if (translatedLineNumbers != null
                && translatedLineNumbers.getVisibility() == View.VISIBLE) {
            translatedLineNumbers.invalidate();
        }
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
                + ";font-family:sans-serif;padding:0;margin:0;background:transparent;overflow-x:auto;}"
                + "p,li,blockquote,td,th,h1,h2,h3,h4,h5,h6,a,span,strong,em{white-space:nowrap;}"
                + "a{color:"
                + linkColor
                + ";}"
                + "pre,code{white-space:pre;background:"
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

    private void hideStatusBar() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller == null) {
            return;
        }
        controller.hide(WindowInsetsCompat.Type.statusBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private static void applySafeInsets(View root) {
        int basePaddingLeft = root.getPaddingLeft();
        int basePaddingTop = root.getPaddingTop();
        int basePaddingRight = root.getPaddingRight();
        int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    Insets safeInsets =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.navigationBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            basePaddingLeft + safeInsets.left,
                            basePaddingTop + safeInsets.top,
                            basePaddingRight + safeInsets.right,
                            basePaddingBottom + safeInsets.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(root);
    }

    private void syncScroll(View source, View target, int sourceScrollX, int sourceScrollY) {
        if (syncingScroll) {
            return;
        }
        int targetMaxScrollX = calculateMaxHorizontalScroll(target);
        int targetMaxScrollY = calculateMaxVerticalScroll(target);
        int clampedTargetX = Math.max(0, Math.min(sourceScrollX, targetMaxScrollX));
        int clampedTargetY = Math.max(0, Math.min(sourceScrollY, targetMaxScrollY));
        syncingScroll = true;
        target.scrollTo(clampedTargetX, clampedTargetY);
        syncingScroll = false;
    }

    private void syncRawScrollByLine(
            TextView source, TextView target, int sourceScrollX, int sourceScrollY) {
        if (syncingScroll) {
            return;
        }
        Layout sourceLayout = source.getLayout();
        Layout targetLayout = target.getLayout();
        if (sourceLayout == null || targetLayout == null) {
            syncScroll(source, target, sourceScrollX, sourceScrollY);
            return;
        }

        int clampedTargetX =
                Math.max(0, Math.min(sourceScrollX, calculateMaxHorizontalScroll(target)));
        int sourceVertical = sourceScrollY + source.getCompoundPaddingTop();
        int sourceLine = sourceLayout.getLineForVertical(sourceVertical);
        int sourceLineTop = sourceLayout.getLineTop(sourceLine);
        int sourceLineBottom = sourceLayout.getLineBottom(sourceLine);
        int sourceLineHeight = Math.max(1, sourceLineBottom - sourceLineTop);
        float sourceLineOffsetRatio = (sourceVertical - sourceLineTop) / (float) sourceLineHeight;
        float clampedRatio = Math.max(0f, Math.min(1f, sourceLineOffsetRatio));

        int targetLine = Math.max(0, Math.min(sourceLine, targetLayout.getLineCount() - 1));
        int targetLineTop = targetLayout.getLineTop(targetLine);
        int targetLineBottom = targetLayout.getLineBottom(targetLine);
        int targetLineHeight = Math.max(1, targetLineBottom - targetLineTop);
        int targetVertical = targetLineTop + Math.round(targetLineHeight * clampedRatio);
        int unclampedTargetY = targetVertical - target.getCompoundPaddingTop();
        int clampedTargetY =
                Math.max(0, Math.min(unclampedTargetY, calculateMaxVerticalScroll(target)));

        syncingScroll = true;
        target.scrollTo(clampedTargetX, clampedTargetY);
        syncingScroll = false;
    }

    private static int calculateMaxHorizontalScroll(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getLayout() == null) {
                return 0;
            }
            int lineCount = textView.getLineCount();
            float widestLine = 0f;
            for (int i = 0; i < lineCount; i++) {
                widestLine = Math.max(widestLine, textView.getLayout().getLineWidth(i));
            }
            int contentWidth = (int) Math.ceil(widestLine);
            int visibleWidth =
                    textView.getWidth()
                            - textView.getCompoundPaddingLeft()
                            - textView.getCompoundPaddingRight();
            return Math.max(0, contentWidth - visibleWidth);
        }
        if (view instanceof WebView) {
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    private static int calculateMaxVerticalScroll(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getLayout() == null) {
                return 0;
            }
            int contentHeight = textView.getLayout().getHeight();
            int visibleHeight =
                    textView.getHeight()
                            - textView.getCompoundPaddingTop()
                            - textView.getCompoundPaddingBottom();
            return Math.max(0, contentHeight - visibleHeight);
        }
        if (view instanceof WebView) {
            WebView webView = (WebView) view;
            int contentHeight = (int) Math.floor(webView.getContentHeight() * webView.getScale());
            int visibleHeight = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
            return Math.max(0, contentHeight - visibleHeight);
        }
        return 0;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideStatusBar();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showRenderToggleTemporarily();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        if (renderToggleButton != null) {
            renderToggleButton.removeCallbacks(hideRenderToggleRunnable);
        }
        super.onDestroy();
    }
}
