package com.fastshoppr.harness;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.common.RecognitionCandidate;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.recognition.Ink;
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext;
import com.google.mlkit.vision.digitalink.recognition.WritingArea;

import java.util.List;

public final class DigitalInkProbeActivity extends Activity {

    private static final String LANGUAGE_TAG = "en-US";

    private TextView statusText;
    private TextView resultText;
    private InkCanvasView inkCanvas;
    private DigitalInkRecognitionModel model;
    private DigitalInkRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_ink);

        statusText = findViewById(R.id.status_text);
        resultText = findViewById(R.id.result_text);
        inkCanvas = findViewById(R.id.ink_canvas);

        findViewById(R.id.download_button).setOnClickListener(v -> ensureModel());
        findViewById(R.id.recognize_button).setOnClickListener(v -> recognize());
        findViewById(R.id.clear_button)
                .setOnClickListener(
                        v -> {
                            inkCanvas.clear();
                            resultText.setText("");
                        });
    }

    private void ensureModel() {
        if (recognizer != null) {
            statusText.setText("English Digital Ink model is ready.");
            return;
        }

        if (model == null) {
            try {
                DigitalInkRecognitionModelIdentifier identifier =
                        DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGUAGE_TAG);
                if (identifier == null) {
                    statusText.setText("No Digital Ink model is available for " + LANGUAGE_TAG + ".");
                    return;
                }
                model = DigitalInkRecognitionModel.builder(identifier).build();
            } catch (MlKitException e) {
                statusText.setText("Model identifier failed: " + e.getMessage());
                return;
            }
        }

        statusText.setText("Checking the English Digital Ink model...");
        RemoteModelManager manager = RemoteModelManager.getInstance();
        manager
                .isModelDownloaded(model)
                .addOnSuccessListener(
                        downloaded -> {
                            if (downloaded) {
                                createRecognizer();
                            } else {
                                downloadModel(manager);
                            }
                        })
                .addOnFailureListener(
                        e -> statusText.setText("Model check failed: " + e.getMessage()));
    }

    private void downloadModel(RemoteModelManager manager) {
        statusText.setText("Downloading English Digital Ink model (official size about 20 MB)...");
        manager
                .download(model, new DownloadConditions.Builder().build())
                .addOnSuccessListener(v -> createRecognizer())
                .addOnFailureListener(
                        e -> statusText.setText("Model download failed: " + e.getMessage()));
    }

    private void createRecognizer() {
        if (recognizer != null) {
            recognizer.close();
        }
        recognizer =
                DigitalInkRecognition.getClient(
                        DigitalInkRecognizerOptions.builder(model).build());
        statusText.setText("Model ready. Write something, then tap Recognize.");
    }

    private void recognize() {
        if (recognizer == null) {
            statusText.setText("Download the model first.");
            return;
        }
        if (inkCanvas.isEmpty()) {
            statusText.setText("Draw at least one stroke first.");
            return;
        }

        Ink ink = inkCanvas.buildInk();
        RecognitionContext context =
                RecognitionContext.builder()
                        .setWritingArea(
                                new WritingArea(inkCanvas.getWidth(), inkCanvas.getHeight()))
                        .build();

        long started = System.nanoTime();
        statusText.setText("Recognizing...");
        recognizer
                .recognize(ink, context)
                .addOnSuccessListener(result -> showResult(result.getCandidates(), started))
                .addOnFailureListener(
                        e -> statusText.setText("Recognition failed: " + e.getMessage()));
    }

    private void showResult(List<RecognitionCandidate> candidates, long started) {
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        if (candidates.isEmpty()) {
            statusText.setText("Recognized in " + elapsedMs + " ms, but no text was returned.");
            resultText.setText("");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            RecognitionCandidate candidate = candidates.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(candidate.getText())
                    .append("  score=")
                    .append(candidate.getScore())
                    .append('\n');
        }
        statusText.setText("Recognized in " + elapsedMs + " ms. Offline after model download.");
        resultText.setText(builder.toString());
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
        super.onDestroy();
    }
}
