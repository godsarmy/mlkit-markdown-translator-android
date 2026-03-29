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
    private static final String EXTRA_FALLBACK_ENABLED = "extra_fallback_enabled";

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
            Context context, String markdown, boolean fallbackModeEnabled) {
        Intent intent = new Intent(context, ExplainMarkdownActivity.class);
        intent.putExtra(EXTRA_MARKDOWN, markdown);
        intent.putExtra(EXTRA_FALLBACK_ENABLED, fallbackModeEnabled);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explain_markdown);

        bindViews();
        String markdown = getIntent().getStringExtra(EXTRA_MARKDOWN);
        boolean fallbackEnabled = getIntent().getBooleanExtra(EXTRA_FALLBACK_ENABLED, true);
        translator =
                new MlKitMarkdownTranslator(
                        new MarkdownTranslationOptions.Builder()
                                .setEnableRegexFallbackProtection(fallbackEnabled)
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
