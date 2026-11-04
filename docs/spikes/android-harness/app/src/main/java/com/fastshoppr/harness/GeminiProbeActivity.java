package com.fastshoppr.harness;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public final class GeminiProbeActivity extends Activity {

    private static final String SYSTEM_INSTRUCTIONS =
            "You clean shopping-list item names. Preserve meaning. Fix obvious spelling errors, "
                    + "expand common abbreviations, and remove list markers. Do not invent brands, "
                    + "sizes, or products. Return JSON only with fields: displayName, confidence, "
                    + "reason. If no change is needed, return displayName equal to the input.";

    private EditText itemInput;
    private TextView statusText;
    private TextView resultText;
    private Button downloadButton;
    private Button runButton;
    private GenerativeModelFutures model;
    private Executor mainExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini);

        itemInput = findViewById(R.id.item_input);
        statusText = findViewById(R.id.status_text);
        resultText = findViewById(R.id.result_text);
        downloadButton = findViewById(R.id.download_button);
        runButton = findViewById(R.id.run_button);
        mainExecutor = ContextCompat.getMainExecutor(this);

        findViewById(R.id.check_button).setOnClickListener(v -> checkAvailability());
        downloadButton.setOnClickListener(v -> downloadModel());
        runButton.setOnClickListener(v -> runPrompt());
    }

    private void ensureModel() {
        if (model != null) {
            return;
        }
        try {
            model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
        } catch (Throwable t) {
            statusText.setText("Could not create the GenAI client: " + t);
        }
    }

    private void checkAvailability() {
        ensureModel();
        if (model == null) {
            return;
        }

        statusText.setText("Checking Gemini Nano / AICore availability...");
        downloadButton.setEnabled(false);
        runButton.setEnabled(false);

        ListenableFuture<Integer> future;
        try {
            future = model.checkStatus();
        } catch (Throwable t) {
            statusText.setText("Availability check failed: " + t);
            return;
        }

        future.addListener(
                () -> {
                    try {
                        int status = future.get();
                        showStatus(status);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        statusText.setText("Availability check interrupted.");
                    } catch (ExecutionException e) {
                        showFailure(e.getCause());
                    }
                },
                mainExecutor);
    }

    private void showStatus(int status) {
        switch (status) {
            case FeatureStatus.AVAILABLE:
                downloadButton.setEnabled(false);
                runButton.setEnabled(true);
                statusText.setText("AVAILABLE. Gemini Nano is ready. Run the cleanup prompt.");
                break;
            case FeatureStatus.DOWNLOADABLE:
                downloadButton.setEnabled(true);
                runButton.setEnabled(false);
                statusText.setText("DOWNLOADABLE. The model is not downloaded yet.");
                break;
            case FeatureStatus.DOWNLOADING:
                downloadButton.setEnabled(false);
                runButton.setEnabled(false);
                statusText.setText("DOWNLOADING. Wait for the model to finish downloading.");
                break;
            case FeatureStatus.UNAVAILABLE:
            default:
                downloadButton.setEnabled(false);
                runButton.setEnabled(false);
                statusText.setText(
                        "UNAVAILABLE. Use the deterministic rules fallback; AI is optional.");
                resultText.setText("Rules fallback:\n" + rulesCleanup(itemInput.getText().toString()));
                break;
        }
    }

    private void downloadModel() {
        if (model == null) {
            return;
        }
        statusText.setText("Downloading the GenAI model...");
        downloadButton.setEnabled(false);

        model.download(
                new DownloadCallback() {
                    @Override
                    public void onDownloadStarted(long bytesToDownload) {
                        statusText.setText(
                                "Download started. Bytes: "
                                        + (bytesToDownload < 0 ? "unknown" : bytesToDownload));
                    }

                    @Override
                    public void onDownloadProgress(long totalBytesDownloaded) {
                        statusText.setText("Downloaded bytes: " + totalBytesDownloaded);
                    }

                    @Override
                    public void onDownloadCompleted() {
                        statusText.setText("Download complete. Checking availability...");
                        checkAvailability();
                    }

                    @Override
                    public void onDownloadFailed(GenAiException e) {
                        statusText.setText(
                                "Download failed: code="
                                        + e.getErrorCode()
                                        + " "
                                        + e.getMessage());
                    }
                });
    }

    private void runPrompt() {
        if (model == null) {
            statusText.setText("Check availability first.");
            return;
        }

        String item = itemInput.getText().toString().trim();
        if (item.isEmpty()) {
            statusText.setText("Enter an item first.");
            return;
        }

        String prompt = SYSTEM_INSTRUCTIONS + "\n\nInput: \"" + item + "\"";
        GenerateContentRequest.Builder requestBuilder =
                new GenerateContentRequest.Builder(new TextPart(prompt));
        requestBuilder.setTemperature(0.0f);
        requestBuilder.setTopK(1);
        requestBuilder.setCandidateCount(1);
        requestBuilder.setMaxOutputTokens(128);
        GenerateContentRequest request = requestBuilder.build();

        statusText.setText("Running the cleanup prompt...");
        runButton.setEnabled(false);
        long started = System.nanoTime();

        ListenableFuture<GenerateContentResponse> future;
        try {
            future = model.generateContent(request);
        } catch (Throwable t) {
            runButton.setEnabled(true);
            statusText.setText("Prompt failed: " + t);
            return;
        }

        future.addListener(
                () -> {
                    runButton.setEnabled(true);
                    long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                    try {
                        GenerateContentResponse response = future.get();
                        String text =
                                response.getCandidates().isEmpty()
                                        ? "(no candidates)"
                                        : response.getCandidates().get(0).getText();
                        statusText.setText(
                                String.format(
                                        Locale.US,
                                        "Gemini Nano responded in %d ms. Foreground-only.",
                                        elapsedMs));
                        resultText.setText(
                                "AI response:\n"
                                        + text
                                        + "\n\nDeterministic rules fallback:\n"
                                        + rulesCleanup(item));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        statusText.setText("Prompt interrupted.");
                    } catch (ExecutionException e) {
                        showFailure(e.getCause());
                        resultText.setText(
                                "Deterministic rules fallback:\n" + rulesCleanup(item));
                    }
                },
                mainExecutor);
    }

    private void showFailure(Throwable cause) {
        if (cause instanceof GenAiException) {
            GenAiException e = (GenAiException) cause;
            statusText.setText(
                    "GenAI error code="
                            + e.getErrorCode()
                            + ", retryAfter="
                            + e.getRetryDelay()
                            + ". Falling back to deterministic rules.");
        } else {
            statusText.setText("GenAI failure: " + cause + ". Falling back to deterministic rules.");
        }
    }

    static String rulesCleanup(String input) {
        if (input == null) {
            return "";
        }
        String cleaned =
                input.trim()
                        .replaceAll("\\s+", " ")
                        .replaceAll("^\\s*(?:[-*]|\\d+[.)])\\s*", "")
                        .replaceAll("(?i)\\btp\\b", "toilet paper")
                        .replaceAll("(?i)\\bpb\\b", "peanut butter")
                        .replaceAll("(?i)\\bgal\\b", "gallon");
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
}
