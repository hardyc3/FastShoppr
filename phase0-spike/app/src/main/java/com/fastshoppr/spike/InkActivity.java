package com.fastshoppr.spike;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

public class InkActivity extends AppCompatActivity {
    private static final String TAG = "PHASE0";
    private InkView inkView;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ink);
        inkView = findViewById(R.id.inkView);
        result = findViewById(R.id.inkResult);
        findViewById(R.id.btnClear).setOnClickListener(v -> { inkView.clear(); result.setText(""); });
        findViewById(R.id.btnRecognize).setOnClickListener(v -> recognize());
        result.setText("Draw a handwritten shopping list (one item per line), then tap Recognize.");
    }

    private void recognize() {
        if (inkView.isEmpty()) { result.setText("Nothing drawn."); return; }
        final DigitalInkRecognitionModel model;
        try {
            model = DigitalInkRecognitionModel.builder(
                    DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")).build();
        } catch (Exception e) {
            result.setText("model build failed: " + e);
            return;
        }
        RemoteModelManager manager = RemoteModelManager.getInstance();
        manager.isModelDownloaded(model).addOnSuccessListener(downloaded -> {
            if (downloaded) {
                runRecognition(model);
            } else {
                result.setText("Downloading en-US model…");
                manager.download(model, new DownloadConditions.Builder().build())
                        .addOnSuccessListener(v -> runRecognition(model))
                        .addOnFailureListener(e -> result.setText("model download FAILED: " + e));
            }
        }).addOnFailureListener(e -> result.setText("isModelDownloaded failed: " + e));
    }

    private void runRecognition(DigitalInkRecognitionModel model) {
        Ink ink = inkView.buildInk();
        int w = Math.max(inkView.getWidth(), 1);
        int h = Math.max(inkView.getHeight(), 1);
        RecognitionContext context = RecognitionContext.builder()
                .setWritingArea(new WritingArea(w, h))
                .setPreContext("")
                .build();
        DigitalInkRecognizer recognizer =
                DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
        long start = System.currentTimeMillis();
        recognizer.recognize(ink, context)
                .addOnSuccessListener(r -> {
                    long ms = System.currentTimeMillis() - start;
                    StringBuilder sb = new StringBuilder();
                    sb.append("latency(ms): ").append(ms).append('\n');
                    List<RecognitionCandidate> candidates = r.getCandidates();
                    for (int i = 0; i < Math.min(10, candidates.size()); i++) {
                        sb.append("candidate[").append(i).append("]: ").append(candidates.get(i).getText()).append('\n');
                    }
                    Log.i(TAG, "InkActivity recognize done, candidates=" + candidates.size());
                    result.setText(sb.toString());
                })
                .addOnFailureListener(e -> result.setText("recognize FAILED: " + e));
    }
}
