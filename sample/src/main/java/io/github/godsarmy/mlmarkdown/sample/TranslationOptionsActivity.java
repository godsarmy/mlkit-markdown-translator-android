package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
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
    private static final String EXTRA_ESCAPED_MARKDOWN_CHARACTERS_TO_PROTECT =
            "extra_escaped_markdown_characters_to_protect";
    private static final String EXTRA_TOKEN_MARKER = "extra_token_marker";
    private static final String EXTRA_MAX_CHARS_PER_CHUNK = "extra_max_chars_per_chunk";
    private static final String EXTRA_TRANSLATE_TIMEOUT_MS = "extra_translate_timeout_ms";
    private static final int DEFAULT_TRANSLATE_TIMEOUT_MS = 0;

    private SwitchMaterial preserveNewlinesSwitch;
    private SwitchMaterial preserveListPrefixesSwitch;
    private SwitchMaterial preserveBlockquotesSwitch;
    private SwitchMaterial normalizeCustomBlockTagsSwitch;
    private SwitchMaterial protectAutolinksSwitch;
    private SwitchMaterial enableRegexFallbackProtectionSwitch;
    private SwitchMaterial preserveWhitespaceAroundProtectedSegmentsSwitch;
    private EditText escapedMarkdownCharactersInput;
    private EditText tokenMarkerInput;
    private EditText maxCharsPerChunkInput;
    private EditText translateTimeoutMsInput;

    public static Intent createIntent(
            Context context, MarkdownTranslationOptions options, int translateTimeoutMs) {
        return withOptions(
                new Intent(context, TranslationOptionsActivity.class), options, translateTimeoutMs);
    }

    public static MarkdownTranslationOptions extractOptions(@Nullable Intent intent) {
        if (intent == null) {
            return new MarkdownTranslationOptions.Builder().build();
        }
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
                .setEscapedMarkdownCharactersToProtect(escapedMarkdownCharactersToProtect)
                .setTokenMarker(tokenMarker)
                .setMaxCharsPerChunk(maxCharsPerChunk)
                .build();
    }

    public static Intent resultIntent(MarkdownTranslationOptions options) {
        return resultIntent(options, DEFAULT_TRANSLATE_TIMEOUT_MS);
    }

    public static Intent resultIntent(MarkdownTranslationOptions options, int translateTimeoutMs) {
        return withOptions(new Intent(), options, translateTimeoutMs);
    }

    public static int extractTranslateTimeoutMs(@Nullable Intent intent) {
        if (intent == null) {
            return DEFAULT_TRANSLATE_TIMEOUT_MS;
        }
        return Math.max(
                DEFAULT_TRANSLATE_TIMEOUT_MS,
                intent.getIntExtra(EXTRA_TRANSLATE_TIMEOUT_MS, DEFAULT_TRANSLATE_TIMEOUT_MS));
    }

    private static Intent withOptions(
            Intent intent, MarkdownTranslationOptions options, int translateTimeoutMs) {
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
        intent.putExtra(
                EXTRA_TRANSLATE_TIMEOUT_MS,
                Math.max(DEFAULT_TRANSLATE_TIMEOUT_MS, translateTimeoutMs));
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translation_options);
        setupToolbar();
        bindViews();
        populateInitialValues();
        setupActions();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.translationOptionsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
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
        escapedMarkdownCharactersInput = findViewById(R.id.escapedMarkdownCharactersInput);
        tokenMarkerInput = findViewById(R.id.tokenMarkerInput);
        maxCharsPerChunkInput = findViewById(R.id.maxCharsPerChunkInput);
        translateTimeoutMsInput = findViewById(R.id.translateTimeoutMsInput);
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
        escapedMarkdownCharactersInput.setText(options.escapedMarkdownCharactersToProtect());
        tokenMarkerInput.setText(options.tokenMarker());
        maxCharsPerChunkInput.setText(String.valueOf(options.maxCharsPerChunk()));
        translateTimeoutMsInput.setText(String.valueOf(extractTranslateTimeoutMs(getIntent())));
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
                    String escapedMarkdownCharactersToProtect =
                            Objects.requireNonNull(escapedMarkdownCharactersInput.getText())
                                    .toString()
                                    .trim();
                    String translateTimeoutMsText =
                            Objects.requireNonNull(translateTimeoutMsInput.getText())
                                    .toString()
                                    .trim();

                    tokenMarkerInput.setError(null);
                    maxCharsPerChunkInput.setError(null);
                    translateTimeoutMsInput.setError(null);

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
                    if (translateTimeoutMsText.isEmpty()) {
                        translateTimeoutMsInput.setError(
                                getString(R.string.translate_timeout_ms_required_error));
                        return;
                    }
                    int parsedTranslateTimeoutMs;
                    try {
                        parsedTranslateTimeoutMs = Integer.parseInt(translateTimeoutMsText);
                    } catch (NumberFormatException numberFormatException) {
                        translateTimeoutMsInput.setError(
                                getString(R.string.translate_timeout_ms_non_negative_error));
                        return;
                    }
                    if (parsedTranslateTimeoutMs < 0) {
                        translateTimeoutMsInput.setError(
                                getString(R.string.translate_timeout_ms_non_negative_error));
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
                                    .setEscapedMarkdownCharactersToProtect(
                                            escapedMarkdownCharactersToProtect)
                                    .setTokenMarker(enteredTokenMarker)
                                    .setMaxCharsPerChunk(parsedMaxChars)
                                    .build();
                    Intent result = resultIntent(options, parsedTranslateTimeoutMs);
                    setResult(RESULT_OK, result);
                    finish();
                });
    }
}
