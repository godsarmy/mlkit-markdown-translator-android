package io.github.godsarmy.mlmarkdown.sample;

final class ExplainPageItem {
    private final String title;
    private final String emptyText;
    private final java.util.List<String> entries;
    private final boolean sourceTab;

    ExplainPageItem(
            String title, String emptyText, java.util.List<String> entries, boolean sourceTab) {
        this.title = title;
        this.emptyText = emptyText;
        this.entries = entries;
        this.sourceTab = sourceTab;
    }

    String getTitle() {
        return title;
    }

    String getEmptyText() {
        return emptyText;
    }

    java.util.List<String> getEntries() {
        return entries;
    }

    boolean isSourceTab() {
        return sourceTab;
    }
}
