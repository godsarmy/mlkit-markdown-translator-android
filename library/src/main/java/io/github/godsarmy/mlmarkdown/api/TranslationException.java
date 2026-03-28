package io.github.godsarmy.mlmarkdown.api;

import androidx.annotation.NonNull;

public final class TranslationException extends Exception {
    private final TranslationErrorCode code;

    public TranslationException(
            TranslationErrorCode code, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public TranslationErrorCode getCode() {
        return code;
    }
}
