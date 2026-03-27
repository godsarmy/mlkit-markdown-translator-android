package io.github.godsarmy.mlmarkdown;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import org.junit.Test;

public class MarkdownTranslationOptionsTest {
    @Test
    public void defaults_enableAllOptions() {
        MarkdownTranslationOptions options = MarkdownTranslationOptions.defaults();

        assertTrue(options.preserveNewlines());
        assertTrue(options.preserveListPrefixes());
        assertTrue(options.preserveBlockquotes());
        assertTrue(options.normalizeCustomBlockTags());
        assertTrue(options.protectAutolinks());
        assertTrue(options.enableRegexFallbackProtection());
        assertTrue(options.preserveWhitespaceAroundProtectedSegments());
        assertNull(options.translationTimingListener());
    }

    @Test
    public void builder_allowsDisablingOptions() {
        MarkdownTranslationOptions options =
                new MarkdownTranslationOptions.Builder()
                        .setPreserveNewlines(false)
                        .setPreserveListPrefixes(false)
                        .setPreserveBlockquotes(false)
                        .setNormalizeCustomBlockTags(false)
                        .setProtectAutolinks(false)
                        .setEnableRegexFallbackProtection(false)
                        .setPreserveWhitespaceAroundProtectedSegments(false)
                        .build();

        assertFalse(options.preserveNewlines());
        assertFalse(options.preserveListPrefixes());
        assertFalse(options.preserveBlockquotes());
        assertFalse(options.normalizeCustomBlockTags());
        assertFalse(options.protectAutolinks());
        assertFalse(options.enableRegexFallbackProtection());
        assertFalse(options.preserveWhitespaceAroundProtectedSegments());
        assertNull(options.translationTimingListener());
    }

    @Test
    public void builder_allowsSettingTimingListener() {
        TranslationTimingListener listener = new NoOpTimingListener();
        MarkdownTranslationOptions options =
                new MarkdownTranslationOptions.Builder()
                        .setTranslationTimingListener(listener)
                        .build();

        assertSame(listener, options.translationTimingListener());
    }

    private static final class NoOpTimingListener implements TranslationTimingListener {
        @Override
        public void onCompleted(TranslationTimingReport report) {
            // no-op
        }
    }
}
