package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;

public class MarkdownRestorer {
    public String restore(String translatedMarkdown, MarkdownTokenStore tokenStore) {
        String restored = translatedMarkdown;
        for (ProtectedSegment segment : tokenStore.getAll()) {
            restored = restored.replace(segment.getToken(), segment.getOriginalText());
        }
        return restored;
    }
}
