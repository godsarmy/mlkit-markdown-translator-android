package io.github.godsarmy.mlmarkdown.sample;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.github.godsarmy.mlmarkdown.MlKitMarkdownTranslator;

import io.noties.markwon.Markwon;

public final class MainActivity extends AppCompatActivity {
    private MlKitMarkdownTranslator translator;
    private Markwon markwon;

    private EditText sourceMarkdownInput;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;
    private TextView statusText;
    private TextView originalMarkdownPreview;
    private TextView translatedMarkdownPreview;
    private TextView renderedMarkdownPreview;
    private Button downloadModelButton;
    private Button translateButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translator = new MlKitMarkdownTranslator();
        markwon = Markwon.create(this);

        bindViews();
        setupLanguageSpinners();
        setupActions();
        updateOriginalPreview();
    }

    private void bindViews() {
        sourceMarkdownInput = findViewById(R.id.sourceMarkdownInput);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        statusText = findViewById(R.id.statusText);
        originalMarkdownPreview = findViewById(R.id.originalMarkdownPreview);
        translatedMarkdownPreview = findViewById(R.id.translatedMarkdownPreview);
        renderedMarkdownPreview = findViewById(R.id.renderedMarkdownPreview);
        downloadModelButton = findViewById(R.id.downloadModelButton);
        translateButton = findViewById(R.id.translateButton);
    }

    private void setupLanguageSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_codes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(adapter);
        targetLanguageSpinner.setAdapter(adapter);

        sourceLanguageSpinner.setSelection(0); // en
        targetLanguageSpinner.setSelection(1); // es
    }

    private void setupActions() {
        downloadModelButton.setOnClickListener(v -> downloadTargetModel());
        translateButton.setOnClickListener(v -> translateMarkdown());
    }

    private void updateOriginalPreview() {
        originalMarkdownPreview.setText(sourceMarkdownInput.getText().toString());
    }

    private void setBusy(boolean busy) {
        downloadModelButton.setEnabled(!busy);
        translateButton.setEnabled(!busy);
    }

    private String sourceLanguage() {
        return String.valueOf(sourceLanguageSpinner.getSelectedItem());
    }

    private String targetLanguage() {
        return String.valueOf(targetLanguageSpinner.getSelectedItem());
    }

    private void downloadTargetModel() {
        setBusy(true);
        statusText.setText("Status: downloading model for " + targetLanguage() + "...");

        translator.ensureLanguageModelDownloaded(targetLanguage(), new io.github.godsarmy.mlmarkdown.api.OperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    statusText.setText("Status: model ready for " + targetLanguage());
                    setBusy(false);
                });
            }

            @Override
            public void onFailure(Exception error) {
                runOnUiThread(() -> {
                    statusText.setText("Status: model download failed - " + error.getMessage());
                    setBusy(false);
                });
            }
        });
    }

    private void translateMarkdown() {
        String markdown = sourceMarkdownInput.getText().toString();
        updateOriginalPreview();
        setBusy(true);
        statusText.setText("Status: translating " + sourceLanguage() + " → " + targetLanguage() + "...");

        translator.translateMarkdown(
                markdown,
                sourceLanguage(),
                targetLanguage(),
                new io.github.godsarmy.mlmarkdown.api.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        runOnUiThread(() -> {
                            translatedMarkdownPreview.setText(translatedText);
                            markwon.setMarkdown(renderedMarkdownPreview, translatedText);
                            statusText.setText("Status: translation complete");
                            setBusy(false);
                        });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        runOnUiThread(() -> {
                            statusText.setText("Status: translation failed - " + error.getMessage());
                            setBusy(false);
                        });
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        translator.close();
    }
}
