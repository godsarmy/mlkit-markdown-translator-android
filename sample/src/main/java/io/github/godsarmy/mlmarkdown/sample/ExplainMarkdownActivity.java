package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownChunk;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownToken;
import io.github.godsarmy.mlmarkdown.api.ExplainProtectedSegment;
import io.github.godsarmy.mlmarkdown.markdown.ProcessingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExplainMarkdownActivity extends AppCompatActivity {
    private static final String EXTRA_MARKDOWN = "extra_markdown";
    private static final String EXTRA_PRESERVE_NEWLINES = "extra_preserve_newlines";
    private static final String EXTRA_PRESERVE_LIST_PREFIXES = "extra_preserve_list_prefixes";
    private static final String EXTRA_PRESERVE_BLOCKQUOTES = "extra_preserve_blockquotes";
    private static final String EXTRA_NORMALIZE_CUSTOM_BLOCK_TAGS =
            "extra_normalize_custom_block_tags";
    private static final String EXTRA_PROTECT_AUTOLINKS = "extra_protect_autolinks";
    private static final String EXTRA_ENABLE_REGEX_FALLBACK_PROTECTION =
            "extra_enable_regex_fallback_protection";
    private static final String EXTRA_PRESERVE_WHITESPACE_AROUND_PROTECTED_SEGMENTS =
            "extra_preserve_whitespace_around_protected_segments";
    private static final String EXTRA_ESCAPED_MARKDOWN_CHARACTERS_TO_PROTECT =
            "extra_escaped_markdown_characters_to_protect";
    private static final String EXTRA_TOKEN_MARKER = "extra_token_marker";
    private static final String EXTRA_MAX_CHARS_PER_CHUNK = "extra_max_chars_per_chunk";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private MlKitMarkdownTranslator translator;
    private View loadingContainer;
    private TextView errorText;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ExplainPagerAdapter pagerAdapter;
    @Nullable private TabLayoutMediator tabLayoutMediator;

    public static Intent createIntent(
            Context context, String markdown, MarkdownTranslationOptions options) {
        Intent intent = new Intent(context, ExplainMarkdownActivity.class);
        intent.putExtra(EXTRA_MARKDOWN, markdown);
        intent.putExtra(EXTRA_PRESERVE_NEWLINES, options.preserveNewlines());
        intent.putExtra(EXTRA_PRESERVE_LIST_PREFIXES, options.preserveListPrefixes());
        intent.putExtra(EXTRA_PRESERVE_BLOCKQUOTES, options.preserveBlockquotes());
        intent.putExtra(EXTRA_NORMALIZE_CUSTOM_BLOCK_TAGS, options.normalizeCustomBlockTags());
        intent.putExtra(EXTRA_PROTECT_AUTOLINKS, options.protectAutolinks());
        intent.putExtra(
                EXTRA_ENABLE_REGEX_FALLBACK_PROTECTION, options.enableRegexFallbackProtection());
        intent.putExtra(
                EXTRA_PRESERVE_WHITESPACE_AROUND_PROTECTED_SEGMENTS,
                options.preserveWhitespaceAroundProtectedSegments());
        intent.putExtra(
                EXTRA_ESCAPED_MARKDOWN_CHARACTERS_TO_PROTECT,
                options.escapedMarkdownCharactersToProtect());
        intent.putExtra(EXTRA_TOKEN_MARKER, options.tokenMarker());
        intent.putExtra(EXTRA_MAX_CHARS_PER_CHUNK, options.maxCharsPerChunk());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explain_markdown);

        bindViews();
        Intent intent = getIntent();
        String markdown = intent.getStringExtra(EXTRA_MARKDOWN);
        boolean preserveNewlines = intent.getBooleanExtra(EXTRA_PRESERVE_NEWLINES, true);
        boolean preserveListPrefixes = intent.getBooleanExtra(EXTRA_PRESERVE_LIST_PREFIXES, true);
        boolean preserveBlockquotes = intent.getBooleanExtra(EXTRA_PRESERVE_BLOCKQUOTES, true);
        boolean normalizeCustomBlockTags =
                intent.getBooleanExtra(EXTRA_NORMALIZE_CUSTOM_BLOCK_TAGS, true);
        boolean protectAutolinks = intent.getBooleanExtra(EXTRA_PROTECT_AUTOLINKS, true);
        boolean enableRegexFallbackProtection =
                intent.getBooleanExtra(EXTRA_ENABLE_REGEX_FALLBACK_PROTECTION, true);
        boolean preserveWhitespaceAroundProtectedSegments =
                intent.getBooleanExtra(EXTRA_PRESERVE_WHITESPACE_AROUND_PROTECTED_SEGMENTS, true);
        String tokenMarker = intent.getStringExtra(EXTRA_TOKEN_MARKER);
        if (tokenMarker == null || tokenMarker.isEmpty()) {
            tokenMarker = MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER;
        }
        String escapedMarkdownCharactersToProtect =
                intent.getStringExtra(EXTRA_ESCAPED_MARKDOWN_CHARACTERS_TO_PROTECT);
        if (escapedMarkdownCharactersToProtect == null) {
            escapedMarkdownCharactersToProtect =
                    MarkdownTranslationOptions.DEFAULT_ESCAPED_MARKDOWN_CHARACTERS;
        }
        int maxCharsPerChunk =
                intent.getIntExtra(
                        EXTRA_MAX_CHARS_PER_CHUNK,
                        MarkdownTranslationOptions.DEFAULT_MAX_CHARS_PER_CHUNK);
        translator =
                new MlKitMarkdownTranslator(
                        new MarkdownTranslationOptions.Builder()
                                .setPreserveNewlines(preserveNewlines)
                                .setPreserveListPrefixes(preserveListPrefixes)
                                .setPreserveBlockquotes(preserveBlockquotes)
                                .setNormalizeCustomBlockTags(normalizeCustomBlockTags)
                                .setProtectAutolinks(protectAutolinks)
                                .setEnableRegexFallbackProtection(enableRegexFallbackProtection)
                                .setPreserveWhitespaceAroundProtectedSegments(
                                        preserveWhitespaceAroundProtectedSegments)
                                .setEscapedMarkdownCharactersToProtect(
                                        escapedMarkdownCharactersToProtect)
                                .setTokenMarker(tokenMarker)
                                .setMaxCharsPerChunk(maxCharsPerChunk)
                                .build());

        if (markdown == null) {
            showError(getString(R.string.explain_error_missing_markdown));
            return;
        }

        loadExplainResult(markdown);
    }

    private void bindViews() {
        loadingContainer = findViewById(R.id.explainLoadingContainer);
        errorText = findViewById(R.id.explainErrorText);
        tabLayout = findViewById(R.id.explainTabLayout);
        viewPager = findViewById(R.id.explainViewPager);
        pagerAdapter = new ExplainPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
    }

    private void loadExplainResult(String markdown) {
        showLoading(true);
        executorService.execute(
                () -> {
                    try {
                        ExplainMarkdownResult result = translator.explainMarkdown(markdown);
                        runOnUiThread(() -> bindExplainResult(result));
                    } catch (RuntimeException error) {
                        runOnUiThread(() -> showError(error.getMessage()));
                    }
                });
    }

    private void bindExplainResult(ExplainMarkdownResult result) {
        showLoading(false);
        errorText.setVisibility(View.GONE);

        List<ExplainPageItem> pages =
                List.of(
                        new ExplainPageItem(
                                getPreparedTabTitle(result.getProcessingMode()),
                                getString(R.string.explain_none),
                                List.of(result.getPreparedMarkdown())),
                        new ExplainPageItem(
                                getString(R.string.explain_tab_chunks, result.getTotalChunkCount()),
                                getString(R.string.explain_chunks_empty),
                                formatChunks(result.getChunks())),
                        new ExplainPageItem(
                                getString(R.string.explain_tab_tokens, result.getTotalTokenCount()),
                                getString(R.string.explain_tokens_empty),
                                formatTokens(result.getTokens())),
                        new ExplainPageItem(
                                getString(
                                        R.string.explain_tab_protected,
                                        result.getProtectedSegments().size()),
                                getString(R.string.explain_protected_empty),
                                formatProtectedSegments(result.getProtectedSegments())));
        pagerAdapter.submit(pages);
        bindTabs();
    }

    private void bindTabs() {
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        tabLayoutMediator =
                new TabLayoutMediator(
                        tabLayout,
                        viewPager,
                        (tab, position) -> tab.setText(pagerAdapter.getTitle(position)));
        tabLayoutMediator.attach();
    }

    private String getPreparedTabTitle(ProcessingMode mode) {
        if (mode == ProcessingMode.AST_TOKEN_STREAM) {
            return getString(R.string.explain_tab_prepared_ast);
        }
        return getString(R.string.explain_tab_prepared_fallback);
    }

    private void showError(String message) {
        showLoading(false);
        String safeMessage = message == null || message.isBlank() ? "Unknown error" : message;
        errorText.setText(getString(R.string.explain_error_template, safeMessage));
        errorText.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        loadingContainer.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private List<String> formatChunks(List<ExplainMarkdownChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (ExplainMarkdownChunk chunk : chunks) {
            String text =
                    "#"
                            + chunk.getIndex()
                            + " • len="
                            + chunk.getPlainTextLength()
                            + " • tokenIds="
                            + chunk.getTokenIds()
                            + "\n"
                            + chunk.getRawText();
            items.add(text);
        }
        return items;
    }

    private List<String> formatTokens(List<ExplainMarkdownToken> tokens) {
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (ExplainMarkdownToken token : tokens) {
            String text =
                    token.getType()
                            + " • id="
                            + token.getTokenId()
                            + " • offsets="
                            + token.getStartOffset()
                            + "-"
                            + token.getEndOffset()
                            + "\n"
                            + token.getValue();
            items.add(text);
        }
        return items;
    }

    private List<String> formatProtectedSegments(List<ExplainProtectedSegment> protectedSegments) {
        if (protectedSegments.isEmpty()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (ExplainProtectedSegment segment : protectedSegments) {
            items.add(segment.getToken() + "\n" + segment.getOriginalText());
        }
        return items;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        executorService.shutdownNow();
        if (translator != null) {
            translator.close();
        }
    }
}
