package com.fastshoppr.harness;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class TextRecognitionProbeActivity extends ComponentActivity {

    private TextView statusText;
    private TextView resultText;
    private ImageView previewImage;
    private TextRecognizer recognizer;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;
    private ActivityResultLauncher<Uri> takePicture;
    private Uri captureUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        statusText = findViewById(R.id.status_text);
        resultText = findViewById(R.id.result_text);
        previewImage = findViewById(R.id.preview_image);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        pickImage =
                registerForActivityResult(
                        new ActivityResultContracts.PickVisualMedia(),
                        uri -> {
                            if (uri != null) {
                                process(uri, "gallery");
                            }
                        });

        takePicture =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        success -> {
                            if (Boolean.TRUE.equals(success) && captureUri != null) {
                                process(captureUri, "camera");
                            }
                        });

        findViewById(R.id.pick_button)
                .setOnClickListener(
                        v ->
                                pickImage.launch(
                                        new PickVisualMediaRequest.Builder()
                                                .setMediaType(
                                                        ActivityResultContracts.PickVisualMedia
                                                                .ImageOnly.INSTANCE)
                                                .build()));

        findViewById(R.id.camera_button).setOnClickListener(v -> takePhoto());
    }

    private void takePhoto() {
        try {
            File directory = new File(getCacheDir(), "images");
            if (!directory.exists() && !directory.mkdirs()) {
                statusText.setText("Could not create the capture directory.");
                return;
            }
            File file = File.createTempFile("handwriting-", ".jpg", directory);
            captureUri =
                    FileProvider.getUriForFile(
                            this, getPackageName() + ".fileprovider", file);
            takePicture.launch(captureUri);
        } catch (IOException | RuntimeException e) {
            statusText.setText("Camera capture unavailable: " + e.getMessage());
        }
    }

    private void process(Uri uri, String source) {
        previewImage.setImageURI(uri);
        statusText.setText("Running Text Recognition v2 (" + source + ")...");

        InputImage image;
        try {
            image = InputImage.fromFilePath(this, uri);
        } catch (IOException e) {
            statusText.setText("Could not read the image: " + e.getMessage());
            return;
        }

        long started = System.nanoTime();
        recognizer
                .process(image)
                .addOnSuccessListener(text -> showResult(text, source, started))
                .addOnFailureListener(
                        e -> statusText.setText("Text recognition failed: " + e.getMessage()));
    }

    private void showResult(Text text, String source, long started) {
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        StringBuilder builder = new StringBuilder();
        builder.append("Source: ").append(source).append('\n');
        builder.append("Elapsed: ").append(elapsedMs).append(" ms\n");
        builder.append("Full text:\n").append(text.getText()).append("\n\n");
        builder.append("Lines and confidence:\n");

        int lineCount = 0;
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                if (lineCount >= 120) {
                    builder.append("... truncated ...\n");
                    break;
                }
                builder.append(
                                String.format(
                                        Locale.US,
                                        "%.2f  %s%n",
                                        line.getConfidence(),
                                        line.getText()))
                        .append('\n');
                lineCount++;
            }
        }

        statusText.setText(
                "Text Recognition v2 finished on-device in "
                        + elapsedMs
                        + " ms. Handwriting is best-effort; review is required.");
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
