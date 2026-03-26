package io.github.godsarmy.mlmarkdown;

public final class MarkdownTranslationOptions {
    private final boolean preserveNewlines;
    private final boolean preserveListPrefixes;
    private final boolean preserveBlockquotes;
    private final boolean normalizeCustomBlockTags;
    private final boolean protectAutolinks;
    private final boolean enableRegexFallbackProtection;

    private MarkdownTranslationOptions(Builder builder) {
        this.preserveNewlines = builder.preserveNewlines;
        this.preserveListPrefixes = builder.preserveListPrefixes;
        this.preserveBlockquotes = builder.preserveBlockquotes;
        this.normalizeCustomBlockTags = builder.normalizeCustomBlockTags;
        this.protectAutolinks = builder.protectAutolinks;
        this.enableRegexFallbackProtection = builder.enableRegexFallbackProtection;
    }

    public static MarkdownTranslationOptions defaults() {
        return new Builder().build();
    }

    public boolean preserveNewlines() {
        return preserveNewlines;
    }

    public boolean preserveListPrefixes() {
        return preserveListPrefixes;
    }

    public boolean preserveBlockquotes() {
        return preserveBlockquotes;
    }

    public boolean normalizeCustomBlockTags() {
        return normalizeCustomBlockTags;
    }

    public boolean protectAutolinks() {
        return protectAutolinks;
    }

    public boolean enableRegexFallbackProtection() {
        return enableRegexFallbackProtection;
    }

    public static final class Builder {
        private boolean preserveNewlines = true;
        private boolean preserveListPrefixes = true;
        private boolean preserveBlockquotes = true;
        private boolean normalizeCustomBlockTags = true;
        private boolean protectAutolinks = true;
        private boolean enableRegexFallbackProtection = true;

        public Builder setPreserveNewlines(boolean preserveNewlines) {
            this.preserveNewlines = preserveNewlines;
            return this;
        }

        public Builder setPreserveListPrefixes(boolean preserveListPrefixes) {
            this.preserveListPrefixes = preserveListPrefixes;
            return this;
        }

        public Builder setPreserveBlockquotes(boolean preserveBlockquotes) {
            this.preserveBlockquotes = preserveBlockquotes;
            return this;
        }

        public Builder setNormalizeCustomBlockTags(boolean normalizeCustomBlockTags) {
            this.normalizeCustomBlockTags = normalizeCustomBlockTags;
            return this;
        }

        public Builder setProtectAutolinks(boolean protectAutolinks) {
            this.protectAutolinks = protectAutolinks;
            return this;
        }

        public Builder setEnableRegexFallbackProtection(boolean enableRegexFallbackProtection) {
            this.enableRegexFallbackProtection = enableRegexFallbackProtection;
            return this;
        }

        public MarkdownTranslationOptions build() {
            return new MarkdownTranslationOptions(this);
        }
    }
}
