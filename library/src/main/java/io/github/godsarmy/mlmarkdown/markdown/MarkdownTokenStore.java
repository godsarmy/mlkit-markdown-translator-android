package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MarkdownTokenStore {
    private final List<ProtectedSegment> protectedSegments = new ArrayList<>();

    public void add(ProtectedSegment protectedSegment) {
        protectedSegments.add(protectedSegment);
    }

    public List<ProtectedSegment> getAll() {
        return Collections.unmodifiableList(protectedSegments);
    }
}
