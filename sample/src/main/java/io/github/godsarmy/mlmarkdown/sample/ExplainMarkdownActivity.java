package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownChunk;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownToken;
import io.github.godsarmy.mlmarkdown.api.ExplainProtectedSegment;
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
    private static final String EXTRA_TOKEN_MARKER = "extra_token_marker";
    private static final String EXTRA_MAX_CHARS_PER_CHUNK = "extra_max_chars_per_chunk";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private MlKitMarkdownTranslator translator;
    private View loadingContainer;
    private TextView errorText;
    private TextView modeValue;
    private TextView countsValue;
    private TextView preparedMarkdownValue;
    private TextView chunksValue;
    private TextView tokensValue;
    private TextView protectedSegmentsValue;

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
        modeValue = findViewById(R.id.explainModeValue);
        countsValue = findViewById(R.id.explainCountsValue);
        preparedMarkdownValue = findViewById(R.id.explainPreparedMarkdownValue);
        chunksValue = findViewById(R.id.explainChunksValue);
        tokensValue = findViewById(R.id.explainTokensValue);
        protectedSegmentsValue = findViewById(R.id.explainProtectedSegmentsValue);
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

        modeValue.setText(String.valueOf(result.getProcessingMode()));
        countsValue.setText(
                getString(
                        R.string.explain_counts_value,
                        result.getTotalTokenCount(),
                        result.getTotalChunkCount(),
                        result.getProtectedSegments().size()));
        preparedMarkdownValue.setText(result.getPreparedMarkdown());
        chunksValue.setText(formatChunks(result.getChunks()));
        tokensValue.setText(formatTokens(result.getTokens()));
        protectedSegmentsValue.setText(formatProtectedSegments(result.getProtectedSegments()));
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

    private String formatChunks(List<ExplainMarkdownChunk> chunks) {
        if (chunks.isEmpty()) {
            return getString(R.string.explain_none);
        }

        StringBuilder builder = new StringBuilder();
        for (ExplainMarkdownChunk chunk : chunks) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("#")
                    .append(chunk.getIndex())
                    .append(" • len=")
                    .append(chunk.getPlainTextLength())
                    .append(" • tokenIds=")
                    .append(chunk.getTokenIds())
                    .append("\n")
                    .append(chunk.getRawText());
        }
        return builder.toString();
    }

    private String formatTokens(List<ExplainMarkdownToken> tokens) {
        if (tokens.isEmpty()) {
            return getString(R.string.explain_none);
        }

        StringBuilder builder = new StringBuilder();
        for (ExplainMarkdownToken token : tokens) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(token.getType())
                    .append(" • id=")
                    .append(token.getTokenId())
                    .append(" • offsets=")
                    .append(token.getStartOffset())
                    .append("-")
                    .append(token.getEndOffset())
                    .append("\n")
                    .append(token.getValue());
        }
        return builder.toString();
    }

    private String formatProtectedSegments(List<ExplainProtectedSegment> protectedSegments) {
        if (protectedSegments.isEmpty()) {
            return getString(R.string.explain_none);
        }

        StringBuilder builder = new StringBuilder();
        for (ExplainProtectedSegment segment : protectedSegments) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(segment.getToken()).append("\n").append(segment.getOriginalText());
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (translator != null) {
            translator.close();
        }
    }
}
