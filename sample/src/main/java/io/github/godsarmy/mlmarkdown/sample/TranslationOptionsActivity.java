package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import java.util.Objects;

public final class TranslationOptionsActivity extends AppCompatActivity {
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

    private SwitchMaterial preserveNewlinesSwitch;
    private SwitchMaterial preserveListPrefixesSwitch;
    private SwitchMaterial preserveBlockquotesSwitch;
    private SwitchMaterial normalizeCustomBlockTagsSwitch;
    private SwitchMaterial protectAutolinksSwitch;
    private SwitchMaterial enableRegexFallbackProtectionSwitch;
    private SwitchMaterial preserveWhitespaceAroundProtectedSegmentsSwitch;
    private EditText tokenMarkerInput;
    private EditText maxCharsPerChunkInput;

    public static Intent createIntent(Context context, MarkdownTranslationOptions options) {
        return withOptions(new Intent(context, TranslationOptionsActivity.class), options);
    }

    public static MarkdownTranslationOptions extractOptions(@Nullable Intent intent) {
        if (intent == null) {
            return new MarkdownTranslationOptions.Builder().build();
        }
        String tokenMarker = intent.getStringExtra(EXTRA_TOKEN_MARKER);
        if (tokenMarker == null || tokenMarker.isEmpty()) {
            tokenMarker = MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER;
        }
        int maxCharsPerChunk =
                intent.getIntExtra(
                        EXTRA_MAX_CHARS_PER_CHUNK,
                        MarkdownTranslationOptions.DEFAULT_MAX_CHARS_PER_CHUNK);
        return new MarkdownTranslationOptions.Builder()
                .setPreserveNewlines(intent.getBooleanExtra(EXTRA_PRESERVE_NEWLINES, true))
                .setPreserveListPrefixes(intent.getBooleanExtra(EXTRA_PRESERVE_LIST_PREFIXES, true))
                .setPreserveBlockquotes(intent.getBooleanExtra(EXTRA_PRESERVE_BLOCKQUOTES, true))
                .setNormalizeCustomBlockTags(
                        intent.getBooleanExtra(EXTRA_NORMALIZE_CUSTOM_BLOCK_TAGS, true))
                .setProtectAutolinks(intent.getBooleanExtra(EXTRA_PROTECT_AUTOLINKS, true))
                .setEnableRegexFallbackProtection(
                        intent.getBooleanExtra(EXTRA_ENABLE_REGEX_FALLBACK_PROTECTION, true))
                .setPreserveWhitespaceAroundProtectedSegments(
                        intent.getBooleanExtra(
                                EXTRA_PRESERVE_WHITESPACE_AROUND_PROTECTED_SEGMENTS, true))
                .setTokenMarker(tokenMarker)
                .setMaxCharsPerChunk(maxCharsPerChunk)
                .build();
    }

    public static Intent resultIntent(MarkdownTranslationOptions options) {
        return withOptions(new Intent(), options);
    }

    private static Intent withOptions(Intent intent, MarkdownTranslationOptions options) {
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
        setContentView(R.layout.activity_translation_options);
        bindViews();
        populateInitialValues();
        setupActions();
    }

    private void bindViews() {
        preserveNewlinesSwitch = findViewById(R.id.switchPreserveNewlines);
        preserveListPrefixesSwitch = findViewById(R.id.switchPreserveListPrefixes);
        preserveBlockquotesSwitch = findViewById(R.id.switchPreserveBlockquotes);
        normalizeCustomBlockTagsSwitch = findViewById(R.id.switchNormalizeCustomBlockTags);
        protectAutolinksSwitch = findViewById(R.id.switchProtectAutolinks);
        enableRegexFallbackProtectionSwitch =
                findViewById(R.id.switchEnableRegexFallbackProtection);
        preserveWhitespaceAroundProtectedSegmentsSwitch =
                findViewById(R.id.switchPreserveWhitespaceAroundProtectedSegments);
        tokenMarkerInput = findViewById(R.id.tokenMarkerInput);
        maxCharsPerChunkInput = findViewById(R.id.maxCharsPerChunkInput);
    }

    private void populateInitialValues() {
        MarkdownTranslationOptions options = extractOptions(getIntent());
        preserveNewlinesSwitch.setChecked(options.preserveNewlines());
        preserveListPrefixesSwitch.setChecked(options.preserveListPrefixes());
        preserveBlockquotesSwitch.setChecked(options.preserveBlockquotes());
        normalizeCustomBlockTagsSwitch.setChecked(options.normalizeCustomBlockTags());
        protectAutolinksSwitch.setChecked(options.protectAutolinks());
        enableRegexFallbackProtectionSwitch.setChecked(options.enableRegexFallbackProtection());
        preserveWhitespaceAroundProtectedSegmentsSwitch.setChecked(
                options.preserveWhitespaceAroundProtectedSegments());
        tokenMarkerInput.setText(options.tokenMarker());
        maxCharsPerChunkInput.setText(String.valueOf(options.maxCharsPerChunk()));
    }

    private void setupActions() {
        MaterialButton cancelButton = findViewById(R.id.translationOptionsCancelButton);
        MaterialButton saveButton = findViewById(R.id.translationOptionsSaveButton);

        cancelButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(
                v -> {
                    String enteredTokenMarker =
                            Objects.requireNonNull(tokenMarkerInput.getText()).toString().trim();
                    String maxCharsText =
                            Objects.requireNonNull(maxCharsPerChunkInput.getText())
                                    .toString()
                                    .trim();

                    tokenMarkerInput.setError(null);
                    maxCharsPerChunkInput.setError(null);

                    if (maxCharsText.isEmpty()) {
                        maxCharsPerChunkInput.setError(
                                getString(R.string.max_chars_per_chunk_required_error));
                        return;
                    }
                    int parsedMaxChars;
                    try {
                        parsedMaxChars = Integer.parseInt(maxCharsText);
                    } catch (NumberFormatException numberFormatException) {
                        maxCharsPerChunkInput.setError(
                                getString(R.string.max_chars_per_chunk_positive_error));
                        return;
                    }
                    if (parsedMaxChars <= 0) {
                        maxCharsPerChunkInput.setError(
                                getString(R.string.max_chars_per_chunk_positive_error));
                        return;
                    }
                    if (enteredTokenMarker.isEmpty()) {
                        tokenMarkerInput.setError(getString(R.string.token_marker_required_error));
                        return;
                    }

                    MarkdownTranslationOptions options =
                            new MarkdownTranslationOptions.Builder()
                                    .setPreserveNewlines(preserveNewlinesSwitch.isChecked())
                                    .setPreserveListPrefixes(preserveListPrefixesSwitch.isChecked())
                                    .setPreserveBlockquotes(preserveBlockquotesSwitch.isChecked())
                                    .setNormalizeCustomBlockTags(
                                            normalizeCustomBlockTagsSwitch.isChecked())
                                    .setProtectAutolinks(protectAutolinksSwitch.isChecked())
                                    .setEnableRegexFallbackProtection(
                                            enableRegexFallbackProtectionSwitch.isChecked())
                                    .setPreserveWhitespaceAroundProtectedSegments(
                                            preserveWhitespaceAroundProtectedSegmentsSwitch
                                                    .isChecked())
                                    .setTokenMarker(enteredTokenMarker)
                                    .setMaxCharsPerChunk(parsedMaxChars)
                                    .build();
                    Intent result = resultIntent(options);
                    setResult(RESULT_OK, result);
                    finish();
                });
    }
}
