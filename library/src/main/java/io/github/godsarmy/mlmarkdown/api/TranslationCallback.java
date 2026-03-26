package io.github.godsarmy.mlmarkdown.api;

public interface TranslationCallback {
    void onSuccess(String translatedText);

    void onFailure(Exception error);
}
