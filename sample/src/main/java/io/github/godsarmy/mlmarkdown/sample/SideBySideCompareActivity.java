package io.github.godsarmy.mlmarkdown.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public final class SideBySideCompareActivity extends AppCompatActivity {
    private static final String EXTRA_SOURCE_MARKDOWN = "extra_source_markdown";
    private static final String EXTRA_TRANSLATED_MARKDOWN = "extra_translated_markdown";
    private EditText sourceText;
    private EditText translatedText;
    private boolean syncingScroll;

    public static Intent createIntent(
            Context context, String sourceMarkdown, String translatedMarkdown) {
        Intent intent = new Intent(context, SideBySideCompareActivity.class);
        intent.putExtra(EXTRA_SOURCE_MARKDOWN, sourceMarkdown);
        intent.putExtra(EXTRA_TRANSLATED_MARKDOWN, translatedMarkdown);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_side_by_side_compare);
        setTitle(R.string.compare_screen_title);

        sourceText = findViewById(R.id.compareSourceText);
        translatedText = findViewById(R.id.compareTranslatedText);

        Intent intent = getIntent();
        String sourceMarkdown = intent.getStringExtra(EXTRA_SOURCE_MARKDOWN);
        String translatedMarkdown = intent.getStringExtra(EXTRA_TRANSLATED_MARKDOWN);
        sourceText.setText(sourceMarkdown == null ? "" : sourceMarkdown);
        translatedText.setText(translatedMarkdown == null ? "" : translatedMarkdown);

        sourceText.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        syncVerticalScroll(sourceText, translatedText, scrollY));
        translatedText.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        syncVerticalScroll(translatedText, sourceText, scrollY));
    }

    private void syncVerticalScroll(EditText source, EditText target, int sourceScrollY) {
        if (syncingScroll) {
            return;
        }
        int targetMaxScrollY = calculateMaxVerticalScroll(target);
        int clampedTargetY = Math.max(0, Math.min(sourceScrollY, targetMaxScrollY));
        syncingScroll = true;
        target.scrollTo(target.getScrollX(), clampedTargetY);
        syncingScroll = false;
    }

    private static int calculateMaxVerticalScroll(EditText editText) {
        if (editText.getLayout() == null) {
            return 0;
        }
        int contentHeight = editText.getLayout().getHeight();
        int visibleHeight =
                editText.getHeight()
                        - editText.getCompoundPaddingTop()
                        - editText.getCompoundPaddingBottom();
        return Math.max(0, contentHeight - visibleHeight);
    }
}
