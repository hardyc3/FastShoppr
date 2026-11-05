package com.fastshoppr.spike;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.DownloadStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.GenerativeModel;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PHASE0";
    private TextView output;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> ocrPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = findViewById(R.id.output);

        openProbe(R.id.btnWMilk, "https://www.walmart.com/search?q=milk", "walmart-milk");
        openProbe(R.id.btnWEggs, "https://www.walmart.com/search?q=eggs", "walmart-eggs");
        openProbe(R.id.btnWPaper, "https://www.walmart.com/search?q=paper+towels", "walmart-paper-towels");
        openProbe(R.id.btnHdMilk, "https://www.homedepot.com/s/milk", "homedepot-milk");
        openProbe(R.id.btnHdPaper, "https://www.homedepot.com/s/paper%20towels", "homedepot-paper-towels");
        openProbe(R.id.btnTarget, "https://www.target.com/s?searchTerm=paper+towels", "target-paper-towels");

        findViewById(R.id.btnHomeDepot).setOnClickListener(v -> runHomeDepotProbe());
        findViewById(R.id.btnInkDraw).setOnClickListener(v -> startActivity(new Intent(this, InkActivity.class)));
        findViewById(R.id.btnOcr).setOnClickListener(v -> runOcrProbe());
        findViewById(R.id.btnNanoStatus).setOnClickListener(v -> nanoStatus());
        findViewById(R.id.btnNanoDownload).setOnClickListener(v -> nanoDownload());
        findViewById(R.id.btnNanoPrompt).setOnClickListener(v -> nanoPrompt());

        ocrPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) runOcrOnUri(uri);
        });
        findViewById(R.id.btnOcrPick).setOnClickListener(v -> ocrPicker.launch("image/*"));

        append("Ready. Results also written to app external files dir.");
    }

    private void openProbe(int buttonId, String url, String label) {
        findViewById(buttonId).setOnClickListener(v -> {
            Intent i = new Intent(this, StoreProbeActivity.class);
            i.putExtra(StoreProbeActivity.EXTRA_URL, url);
            i.putExtra(StoreProbeActivity.EXTRA_LABEL, label);
            startActivity(i);
        });
    }

    private void append(String line) {
        Log.i(TAG, line);
        main.post(() -> output.append("\n" + line));
    }

    private void saveResult(String name, String content) {
        try {
            File dir = getExternalFilesDir("phase0");
            if (dir == null) return;
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
            append("saved " + f.getName());
        } catch (Exception e) {
            append("save failed: " + e);
        }
    }

    // ---------- Home Depot HTTP probe (automated, expect 403) ----------

    private void runHomeDepotProbe() {
        append("Home Depot probe start (OkHttp)");
        io.execute(() -> {
            StringBuilder sb = new StringBuilder();
            OkHttpClient client = new OkHttpClient();
            String[] urls = {
                "https://www.homedepot.com/s/milk",
                "https://www.homedepot.com/s/paper%20towels"
            };
            for (String u : urls) {
                try {
                    Request req = new Request.Builder().url(u)
                            .header("User-Agent", "FastShoppr/0.0 (feasibility spike; local test)")
                            .build();
                    try (Response resp = client.newCall(req).execute()) {
                        String body = resp.body() == null ? "" : resp.body().string();
                        sb.append("URL: ").append(u).append('\n')
                          .append("code: ").append(resp.code()).append('\n')
                          .append("bodyLen: ").append(body.length()).append('\n');
                        Document doc = Jsoup.parse(body);
                        sb.append("hasNextData: ").append(doc.selectFirst("script#__NEXT_DATA__") != null).append('\n');
                        sb.append("title: ").append(doc.title()).append("\n\n");
                    }
                } catch (Exception e) {
                    sb.append("URL: ").append(u).append(" error: ").append(e).append("\n\n");
                }
            }
            saveResult("homedepot-probe.txt", sb.toString());
            append("Home Depot probe done");
        });
    }

    // ---------- OCR on a picked photo ----------

    private void runOcrOnUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp == null) { append("OCR: could not decode image"); return; }
            recognizeText(InputImage.fromBitmap(bmp, 0), "ocr-photo-probe.txt");
        } catch (Exception e) {
            append("OCR open failed: " + e);
        }
    }

    // ---------- OCR on a synthetic cursive bitmap ----------

    private void runOcrProbe() {
        Bitmap bmp = Bitmap.createBitmap(1000, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(64f);
        paint.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        canvas.drawText("milk", 60f, 120f, paint);
        canvas.drawText("eggs", 60f, 260f, paint);
        canvas.drawText("paper towels", 60f, 400f, paint);
        canvas.drawText("batteries", 60f, 540f, paint);
        recognizeText(InputImage.fromBitmap(bmp, 0), "ocr-probe.txt");
    }

    private void recognizeText(InputImage image, String outFile) {
        append("OCR start");
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        long start = System.currentTimeMillis();
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String text = result.getText();
                    String info = "latency(ms): " + (System.currentTimeMillis() - start) + "\nrecognized:\n" + text;
                    saveResult(outFile, info);
                    append("OCR done: " + text.replace('\n', '|'));
                })
                .addOnFailureListener(e -> {
                    append("OCR FAILED: " + e);
                    saveResult(outFile, "OCR FAILED: " + e);
                });
    }

    // ---------- Gemini Nano ----------

    private GenerativeModelFutures nano() {
        GenerativeModel model = Generation.INSTANCE.getClient();
        return GenerativeModelFutures.from(model);
    }

    private static String statusName(Integer code) {
        if (code == null) return "null";
        switch (code) {
            case 0: return "UNAVAILABLE (0)";
            case 1: return "DOWNLOADABLE (1)";
            case 2: return "DOWNLOADING (2)";
            case 3: return "AVAILABLE (3)";
            default: return "UNKNOWN (" + code + ")";
        }
    }

    private void nanoStatus() {
        append("Nano: checkStatus ...");
        ListenableFuture<Integer> future = nano().checkStatus();
        Futures.addCallback(future, new com.google.common.util.concurrent.FutureCallback<Integer>() {
            @Override public void onSuccess(Integer code) {
                String s = "Nano status: " + statusName(code);
                append(s);
                saveResult("nano-status.txt", s);
            }
            @Override public void onFailure(Throwable t) {
                String s = "Nano status FAILED: " + t + (t.getCause() != null ? " cause=" + t.getCause() : "");
                append(s);
                saveResult("nano-status.txt", s);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void nanoDownload() {
        append("Nano: download started ...");
        ListenableFuture<Void> future = nano().download(new DownloadCallback() {
            @Override public void onDownloadStarted(long bytesDownloaded) { append("Nano download started"); }
            @Override public void onDownloadProgress(long bytesDownloaded) { append("Nano download progress: " + bytesDownloaded + " bytes"); }
            @Override public void onDownloadCompleted() { append("Nano download completed"); }
            @Override public void onDownloadFailed(GenAiException e) { append("Nano download FAILED: " + e + " code=" + e.getErrorCode()); }
        });
        Futures.addCallback(future, new com.google.common.util.concurrent.FutureCallback<Void>() {
            @Override public void onSuccess(Void v) { append("Nano download future OK"); }
            @Override public void onFailure(Throwable t) { append("Nano download future FAILED: " + t); }
        }, ContextCompat.getMainExecutor(this));
    }

    private void nanoPrompt() {
        String prompt = "Return only the cleaned product name for this shopping item, lowercase, no extra words: 'milk 2% gallon organic valley'";
        append("Nano: prompt ...");
        long start = System.currentTimeMillis();
        ListenableFuture<GenerateContentResponse> future =
                nano().generateContent(prompt);
        Futures.addCallback(future, new com.google.common.util.concurrent.FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse resp) {
                long ms = System.currentTimeMillis() - start;
                String text = resp.getCandidates().isEmpty() ? "<no candidates>" : resp.getCandidates().get(0).getText();
                String s = "Nano latency(ms): " + ms + "\nNano response: " + text;
                append(s);
                saveResult("nano-prompt.txt", s);
            }
            @Override public void onFailure(Throwable t) {
                String s = "Nano generateContent FAILED after " + (System.currentTimeMillis() - start) + " ms: " + t;
                if (t instanceof GenAiException) s += " errorCode=" + ((GenAiException) t).getErrorCode();
                append(s);
                saveResult("nano-prompt.txt", s);
            }
        }, ContextCompat.getMainExecutor(this));
    }
}
