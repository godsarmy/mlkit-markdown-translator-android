package io.github.godsarmy.mlmarkdown;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarkdownTranslationOptionsTest {
    @Test
    public void defaults_enableAllOptions() {
        MarkdownTranslationOptions options = MarkdownTranslationOptions.defaults();

        assertTrue(options.preserveNewlines());
        assertTrue(options.preserveListPrefixes());
        assertTrue(options.preserveBlockquotes());
        assertTrue(options.normalizeCustomBlockTags());
        assertTrue(options.protectAutolinks());
    }

    @Test
    public void builder_allowsDisablingOptions() {
        MarkdownTranslationOptions options = new MarkdownTranslationOptions.Builder()
                .setPreserveNewlines(false)
                .setPreserveListPrefixes(false)
                .setPreserveBlockquotes(false)
                .setNormalizeCustomBlockTags(false)
                .setProtectAutolinks(false)
                .build();

        assertFalse(options.preserveNewlines());
        assertFalse(options.preserveListPrefixes());
        assertFalse(options.preserveBlockquotes());
        assertFalse(options.normalizeCustomBlockTags());
        assertFalse(options.protectAutolinks());
    }
}
