package io.github.godsarmy.mlmarkdown.api;

import java.util.List;

public interface LanguagePacksCallback {
    void onSuccess(List<String> languageCodes);

    void onFailure(Exception error);
}
