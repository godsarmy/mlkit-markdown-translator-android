package io.github.godsarmy.mlmarkdown;

import androidx.annotation.Nullable;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import java.util.Objects;

public final class MarkdownTranslationOptions {
    public static final String DEFAULT_TOKEN_MARKER = "@@";

    private final boolean preserveNewlines;
    private final boolean preserveListPrefixes;
    private final boolean preserveBlockquotes;
    private final boolean normalizeCustomBlockTags;
    private final boolean protectAutolinks;
    private final boolean enableRegexFallbackProtection;
    private final boolean preserveWhitespaceAroundProtectedSegments;
    private final String tokenMarker;
    @Nullable private final TranslationTimingListener translationTimingListener;

    private MarkdownTranslationOptions(Builder builder) {
        this.preserveNewlines = builder.preserveNewlines;
        this.preserveListPrefixes = builder.preserveListPrefixes;
        this.preserveBlockquotes = builder.preserveBlockquotes;
        this.normalizeCustomBlockTags = builder.normalizeCustomBlockTags;
        this.protectAutolinks = builder.protectAutolinks;
        this.enableRegexFallbackProtection = builder.enableRegexFallbackProtection;
        this.preserveWhitespaceAroundProtectedSegments =
                builder.preserveWhitespaceAroundProtectedSegments;
        this.tokenMarker = builder.tokenMarker;
        this.translationTimingListener = builder.translationTimingListener;
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

    public boolean preserveWhitespaceAroundProtectedSegments() {
        return preserveWhitespaceAroundProtectedSegments;
    }

    public String tokenMarker() {
        return tokenMarker;
    }

    @Nullable
    public TranslationTimingListener translationTimingListener() {
        return translationTimingListener;
    }

    public static final class Builder {
        private boolean preserveNewlines = true;
        private boolean preserveListPrefixes = true;
        private boolean preserveBlockquotes = true;
        private boolean normalizeCustomBlockTags = true;
        private boolean protectAutolinks = true;
        private boolean enableRegexFallbackProtection = true;
        private boolean preserveWhitespaceAroundProtectedSegments = true;
        private String tokenMarker = DEFAULT_TOKEN_MARKER;
        @Nullable private TranslationTimingListener translationTimingListener;

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

        public Builder setPreserveWhitespaceAroundProtectedSegments(
                boolean preserveWhitespaceAroundProtectedSegments) {
            this.preserveWhitespaceAroundProtectedSegments =
                    preserveWhitespaceAroundProtectedSegments;
            return this;
        }

        public Builder setTokenMarker(String tokenMarker) {
            String value = Objects.requireNonNull(tokenMarker, "tokenMarker == null");
            if (value.isEmpty()) {
                throw new IllegalArgumentException("tokenMarker must not be empty");
            }
            this.tokenMarker = value;
            return this;
        }

        public Builder setTranslationTimingListener(
                @Nullable TranslationTimingListener translationTimingListener) {
            this.translationTimingListener = translationTimingListener;
            return this;
        }

        public MarkdownTranslationOptions build() {
            return new MarkdownTranslationOptions(this);
        }
    }
}
