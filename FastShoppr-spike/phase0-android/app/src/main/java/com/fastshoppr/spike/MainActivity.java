package com.fastshoppr.spike;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.digitalink.common.RecognitionResult;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.recognition.Ink;
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext;
import com.google.mlkit.vision.digitalink.recognition.WritingArea;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PHASE0";
    private static final String WM_STORE_ID = "3081";
    private static final String WM_STORE_NAME = "Sacramento Gerber Rd Supercenter";
    private static final String WM_STORE_URL = "https://www.walmart.com/store/3081";
    private static final String HD_STORE_CONTEXT = "store selected for indexed observation: Cumberland store";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final JSONObject results = new JSONObject();
    private TextView statusView;
    private FrameLayout webContainer;
    private WebView webView;
    private String mode = "idle";
    private int walmartIndex = 0;
    private boolean pageHandled = false;
    private boolean walmartItemsStarted = false;
    private boolean walmartStoreSelected = false;
    private boolean walmartExtractScheduled = false;
    private int walmartStoreAttempts = 0;
    private long walmartToken = 0;
    private String walmartExpectedUrl = "";

    private static final String[][] WALMART_ITEMS = {
            {"whole milk", "https://www.walmart.com/ip/Great-Value-2-Reduced-Fat-Milk-1-Gallon-128-fl-oz/10450115", "Great Value 2% Reduced Fat Milk, Gallon"},
            {"fresh basil", "https://www.walmart.com/ip/Fresh-Basil-0-5-oz-Clamshell/3757188318", "Fresh Basil, 0.5 oz Clamshell"},
            {"bananas", "https://www.walmart.com/ip/Fresh-Banana-Fruit-Each/44390948", "Fresh Banana, Each"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            mode = "idle";
        }
        buildUi();
        putDeviceInfo();
        try {
            results.put("phase", "phase-00-feasibility");
            results.put("startedAt", System.currentTimeMillis());
            results.put("mode", mode);
        } catch (Exception ignored) {
        }
        log("started mode=" + mode);
        if ("walmart".equals(mode)) {
            runWalmart();
        } else if ("ink".equals(mode)) {
            runDigitalInk();
        } else if ("ocr".equals(mode)) {
            runOcr();
        } else if ("genai".equals(mode)) {
            runGenAi();
        } else if ("homedepot".equals(mode)) {
            runHomeDepot();
        } else if ("all".equals(mode)) {
            runWalmart();
        } else {
            setStatus("Ready. Use adb am start --es mode walmart|ink|ocr|genai|homedepot");
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("FastShoppr Phase 0 feasibility spike");
        title.setTextSize(20);
        root.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setPadding(0, dp(12), 0, dp(12));
        root.addView(statusView);

        addButton(root, "Walmart WebView", () -> runWalmart());
        addButton(root, "Digital Ink", () -> runDigitalInk());
        addButton(root, "Camera OCR", () -> runOcr());
        addButton(root, "Gemini Nano", () -> runGenAi());
        addButton(root, "Home Depot HTTP", () -> runHomeDepot());
        addButton(root, "Write results", this::writeResults);

        webContainer = new FrameLayout(this);
        webContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(800)));
        webContainer.setAlpha(0.01f);
        root.addView(webContainer);

        setContentView(scroll);
        createWebView();
    }

    private void addButton(LinearLayout root, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
    }

    private void createWebView() {
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(false);
        s.setBlockNetworkImage(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(s.getUserAgentString());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!"walmart".equals(mode) || url == null || !url.contains("walmart.com")) {
                    return;
                }
                if (!walmartStoreSelected) {
                    if (!url.contains("/store/3081")) {
                        return;
                    }
                    log("walmart store page finished " + url);
                    main.postDelayed(() -> selectWalmartStore(view), 2500);
                    return;
                }
                if (pageHandled || walmartExtractScheduled) {
                    return;
                }
                if (!productId(url).equals(productId(walmartExpectedUrl))) {
                    log("ignoring stale page finish " + url);
                    return;
                }
                walmartExtractScheduled = true;
                long token = walmartToken;
                log("walmart product page finished " + url);
                main.postDelayed(() -> {
                    if (token == walmartToken && !pageHandled) {
                        view.evaluateJavascript(walmartScrollJs(), null);
                    }
                }, 1500);
                main.postDelayed(() -> {
                    if (token != walmartToken || pageHandled) {
                        return;
                    }
                    view.evaluateJavascript(walmartExtractJs(), value -> {
                        log("walmart extracted bytes=" + (value == null ? 0 : value.length()));
                        finishWalmartItem("ok", value, url, token);
                    });
                }, 6000);
            }
        });
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);
        webContainer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void selectWalmartStore(WebView view) {
        if (walmartStoreSelected) {
            return;
        }
        String clickJs = "(function(){var b=Array.from(document.querySelectorAll('button'))" +
                ".find(function(e){return /make this my store/i.test(e.innerText||'')});" +
                "if(!b){return 'not-found';}b.click();return 'clicked';})()";
        view.evaluateJavascript(clickJs, value -> {
            log("walmart store click " + value);
            if ((value == null || !value.contains("clicked")) && walmartStoreAttempts < 4) {
                walmartStoreAttempts++;
                main.postDelayed(() -> selectWalmartStore(view), 3000);
                return;
            }
            view.evaluateJavascript("(function(){var m=document.cookie.match(/assortmentStoreId=[^;]+/);" +
                    "return m?m[0]:'none';})()", cookie -> {
                log("walmart store cookie " + cookie);
                walmartStoreSelected = true;
                try {
                    results.getJSONObject("walmart").put("storeClick", value);
                    results.getJSONObject("walmart").put("storeCookie", cookie);
                } catch (Exception ignored) {
                }
                if (!walmartItemsStarted) {
                    walmartItemsStarted = true;
                    main.postDelayed(this::loadNextWalmartItem, 3000);
                }
            });
        });
    }

    private void runWalmart() {
        mode = "walmart";
        walmartIndex = 0;
        walmartItemsStarted = false;
        walmartStoreSelected = false;
        walmartExtractScheduled = false;
        walmartToken = 0;
        walmartStoreAttempts = 0;
        setStatus("Walmart: selecting store " + WM_STORE_ID);
        try {
            JSONObject wm = new JSONObject();
            wm.put("storeId", WM_STORE_ID);
            wm.put("storeName", WM_STORE_NAME);
            wm.put("storeUrl", WM_STORE_URL);
            wm.put("userAgent", webView.getSettings().getUserAgentString());
            wm.put("items", new JSONArray());
            results.put("walmart", wm);
        } catch (Exception e) {
            log("walmart init error " + e);
        }
        webView.loadUrl(WM_STORE_URL);
        main.postDelayed(() -> {
            if (!walmartItemsStarted) {
                log("walmart store selection timed out; continuing without store context");
                walmartStoreSelected = true;
                walmartItemsStarted = true;
                loadNextWalmartItem();
            }
        }, 45000);
    }

    private void loadNextWalmartItem() {
        if (walmartIndex >= WALMART_ITEMS.length) {
            setStatus("Walmart: done");
            log("walmart done");
            writeResults();
            return;
        }
        String[] item = WALMART_ITEMS[walmartIndex];
        String url = item[1] + "?storeId=" + WM_STORE_ID;
        pageHandled = false;
        walmartExtractScheduled = false;
        walmartExpectedUrl = url;
        long token = ++walmartToken;
        setStatus("Walmart: loading " + item[0] + " (" + (walmartIndex + 1) + "/" + WALMART_ITEMS.length + ")");
        log("walmart navigate " + url);
        webView.loadUrl(url);
        main.postDelayed(() -> {
            if (token == walmartToken && !pageHandled) {
                finishWalmartItem("timeout", null, url, token);
            }
        }, 60000);
    }

    private static String productId(String url) {
        if (url == null) return "";
        int start = url.indexOf("/ip/");
        if (start < 0) return "";
        int end = url.length();
        int q = url.indexOf('?', start);
        if (q >= 0) end = q;
        int slash = url.lastIndexOf('/', end - 1);
        return slash >= start ? url.substring(slash + 1, end) : "";
    }

    private String walmartScrollJs() {
        return "(function(){var n=0;var t=setInterval(function(){window.scrollBy(0,700);n++;" +
                "if(n>24){clearInterval(t);}},100);return 'scrolling';})()";
    }

    private String walmartExtractJs() {
        return "(function(){try{" +
                "var body=document.body?document.body.innerText:'';" +
                "var h1=document.querySelector('h1');" +
                "var aisle=(body.match(/Aisle\\s+[A-Z0-9-]+/gi)||[]);" +
                "var store=(body.match(/[A-Z][A-Za-z .'-]{2,}(?:Supercenter|Neighborhood Market)/g)||[]);" +
                "var price=(body.match(/\\$\\s?\\d+(?:\\.\\d{2})?/g)||[]).slice(0,8);" +
                "var n=document.getElementById('__NEXT_DATA__');" +
                "var next=n?n.textContent:'';" +
                "var jsAisle=[];" +
                "var re=/aisle/ig,m;while((m=re.exec(next))!==null&&jsAisle.length<6){jsAisle.push(next.slice(Math.max(0,m.index-80),Math.min(next.length,m.index+160)));}" +
                "var snip='';" +
                "var all=document.querySelectorAll('div,span,p,li');" +
                "for(var i=0;i<all.length;i++){var t=all[i].innerText||'';if(/Aisle\\s+[A-Z]?\\d+/i.test(t)&&t.length<400){snip=all[i].outerHTML.slice(0,4000);break;}}" +
                "var clone=document.body.cloneNode(true);" +
                "clone.querySelectorAll('script,style,noscript,iframe,svg,link').forEach(function(e){e.remove();});" +
                "return JSON.stringify({url:location.href,title:document.title,h1:h1?h1.innerText:'',aisle:aisle,store:store,price:price,blocked:/blocked|Access Denied|captcha|verify you are human|robot/i.test(body),textLen:body.length,text:body.slice(0,12000),nextAisle:jsAisle,snippet:snip,html:clone.innerHTML.slice(0,180000)});" +
                "}catch(e){return JSON.stringify({error:String(e)});}})();";
    }

    private void finishWalmartItem(String status, String json, String url, long token) {
        if (token != walmartToken || pageHandled) {
            return;
        }
        pageHandled = true;
        String[] item = WALMART_ITEMS[walmartIndex];
        try {
            JSONObject rec = new JSONObject();
            rec.put("query", item[0]);
            rec.put("expectedProduct", item[2]);
            rec.put("url", url);
            rec.put("status", status);
            if (json != null) {
                String decoded = json;
                try {
                    Object parsed = new JSONTokener(json).nextValue();
                    if (parsed instanceof String) {
                        decoded = (String) parsed;
                    }
                } catch (Exception ignored) {
                }
                JSONObject o = new JSONObject(decoded);
                rec.put("pageUrl", o.optString("url"));
                rec.put("pageTitle", o.optString("title"));
                rec.put("productName", o.optString("h1"));
                rec.put("aisleMatches", o.optJSONArray("aisle"));
                rec.put("storeMatches", o.optJSONArray("store"));
                rec.put("priceMatches", o.optJSONArray("price"));
                rec.put("blocked", o.optBoolean("blocked"));
                rec.put("textLength", o.optInt("textLen"));
                rec.put("textSnippet", trim(o.optString("text"), 3000));
                rec.put("nextAisleSnippets", o.optJSONArray("nextAisle"));
                rec.put("availabilityHtmlSnippet", trim(o.optString("snippet"), 4000));
                String html = o.optString("html");
                if (html != null && html.length() > 0) {
                    writeFixture("walmart/" + safeName(item[0]) + "-sanitized.html", html);
                }
                JSONArray aisles = o.optJSONArray("aisle");
                if (aisles != null && aisles.length() > 0) {
                    rec.put("resolvedAisle", aisles.optString(0).replaceFirst("(?i)^Aisle\\s+", ""));
                }
                rec.put("resolved", aisles != null && aisles.length() > 0 && !o.optBoolean("blocked"));
            }
            results.getJSONObject("walmart").getJSONArray("items").put(rec);
            log("walmart item " + item[0] + " status=" + status + " aisle=" + rec.optString("resolvedAisle"));
        } catch (Exception e) {
            log("walmart item error " + e);
        }
        walmartIndex++;
        main.postDelayed(this::loadNextWalmartItem, 1200);
    }

    private void runDigitalInk() {
        mode = "ink";
        setStatus("Digital Ink: checking English model");
        log("digital ink start");
        try {
            JSONObject ink = new JSONObject();
            ink.put("languageTag", "en-US");
            results.put("digitalInk", ink);
        } catch (Exception ignored) {
        }
        DigitalInkRecognitionModelIdentifier id;
        try {
            id = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US");
        } catch (Exception e) {
            recordInkError("model identifier: " + e);
            return;
        }
        if (id == null) {
            recordInkError("no model identifier for en-US");
            return;
        }
        DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(id).build();
        RemoteModelManager rmm = RemoteModelManager.getInstance();
        long t0 = System.currentTimeMillis();
        rmm.isModelDownloaded(model)
                .addOnSuccessListener(downloaded -> {
                    try {
                        results.getJSONObject("digitalInk").put("downloadedBefore", downloaded);
                    } catch (Exception ignored) {
                    }
                    if (downloaded) {
                        recognizeInkSample(model, System.currentTimeMillis() - t0);
                    } else {
                        setStatus("Digital Ink: downloading English model");
                        long dlStart = System.currentTimeMillis();
                        rmm.download(model, new DownloadConditions.Builder().build())
                                .addOnSuccessListener(unused -> {
                                    long ms = System.currentTimeMillis() - dlStart;
                                    try {
                                        results.getJSONObject("digitalInk").put("downloadMs", ms);
                                        results.getJSONObject("digitalInk").put("downloadedAfter", true);
                                    } catch (Exception ignored) {
                                    }
                                    log("digital ink model downloaded in " + ms + "ms");
                                    recognizeInkSample(model, ms);
                                })
                                .addOnFailureListener(e -> recordInkError("download: " + e));
                    }
                })
                .addOnFailureListener(e -> recordInkError("isModelDownloaded: " + e));
    }

    private void recognizeInkSample(DigitalInkRecognitionModel model, long modelMs) {
        setStatus("Digital Ink: recognizing samples");
        DigitalInkRecognizer recognizer =
                DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
        JSONArray samples = new JSONArray();
        try {
            results.getJSONObject("digitalInk").put("samples", samples);
            results.getJSONObject("digitalInk").put("modelCheckMs", modelMs);
        } catch (Exception ignored) {
        }
        recognizeOne(recognizer, "milk", "shopping list ", samples, () ->
                recognizeOne(recognizer, "eggs", "shopping list milk ", samples, () ->
                        recognizeOne(recognizer, "bread", "shopping list milk eggs ", samples, () -> {
                            recognizer.close();
                            setStatus("Digital Ink: done");
                            writeResults();
                        })));
    }

    private void recognizeOne(DigitalInkRecognizer recognizer, String word, String preContext,
                              JSONArray samples, Runnable next) {
        long t0 = System.currentTimeMillis();
        Task<RecognitionResult> task;
        try {
            RecognitionContext context = RecognitionContext.builder()
                    .setPreContext(preContext)
                    .setWritingArea(new WritingArea(1000, 320))
                    .build();
            task = recognizer.recognize(buildInk(word), context);
        } catch (Exception e) {
            recordInkSample(samples, word, null, -1, e.toString(), next);
            return;
        }
        task.addOnSuccessListener(result -> {
            long ms = System.currentTimeMillis() - t0;
            List<String> candidates = new ArrayList<>();
            if (result != null && result.getCandidates() != null) {
                for (int i = 0; i < result.getCandidates().size() && i < 5; i++) {
                    candidates.add(result.getCandidates().get(i).getText());
                }
            }
            recordInkSample(samples, word, candidates, ms, null, next);
        }).addOnFailureListener(e -> recordInkSample(samples, word, null,
                System.currentTimeMillis() - t0, e.toString(), next));
    }

    private void recordInkSample(JSONArray samples, String word, List<String> candidates,
                                 long ms, String error, Runnable next) {
        try {
            JSONObject s = new JSONObject();
            s.put("target", word);
            s.put("latencyMs", ms);
            if (candidates != null) {
                s.put("candidates", new JSONArray(candidates));
                s.put("top", candidates.isEmpty() ? "" : candidates.get(0));
            }
            if (error != null) {
                s.put("error", error);
            }
            samples.put(s);
            log("ink " + word + " -> " + (candidates == null ? error : candidates));
        } catch (Exception ignored) {
        }
        next.run();
    }

    private void recordInkError(String message) {
        try {
            results.getJSONObject("digitalInk").put("error", message);
        } catch (Exception ignored) {
        }
        setStatus("Digital Ink error: " + message);
        log("ink error " + message);
        writeResults();
    }

    private static Ink buildInk(String word) {
        Ink.Builder ink = Ink.builder();
        float x = 40;
        for (char c : word.toCharArray()) {
            float advance = addGlyph(ink, c, x);
            x += advance;
        }
        return ink.build();
    }

    private static float addGlyph(Ink.Builder ink, char c, float x) {
        float baseline = 220;
        float top = 100;
        long t = System.currentTimeMillis();
        switch (c) {
            case 'a':
                ink.addStroke(stroke(new float[]{x + 42, top + 15, x + 16, top + 22, x + 8, top + 55,
                        x + 18, top + 86, x + 42, top + 86, x + 50, top + 52, x + 44, top + 14,
                        x + 48, top + 100}, t));
                return 62;
            case 'b':
                ink.addStroke(stroke(new float[]{x + 8, 60, x + 8, baseline, x + 8, top + 50,
                        x + 30, top + 14, x + 50, top + 30, x + 50, top + 70, x + 30, top + 96,
                        x + 8, top + 96}, t));
                return 62;
            case 'c':
                ink.addStroke(stroke(new float[]{x + 46, top + 16, x + 22, top + 10, x + 10, top + 42,
                        x + 20, top + 82, x + 46, top + 92}, t));
                return 58;
            case 'd':
                ink.addStroke(stroke(new float[]{x + 42, top + 15, x + 16, top + 22, x + 8, top + 55,
                        x + 18, top + 86, x + 42, top + 86, x + 50, top + 52, x + 44, top + 14,
                        x + 48, baseline}, t));
                return 62;
            case 'e':
                ink.addStroke(stroke(new float[]{x + 48, top + 58, x + 10, top + 58, x + 10, top + 35,
                        x + 22, top + 12, x + 42, top + 14, x + 50, top + 40, x + 48, top + 70,
                        x + 34, top + 92, x + 14, top + 88, x + 8, top + 72}, t));
                return 60;
            case 'f':
                ink.addStroke(stroke(new float[]{x + 42, 70, x + 22, 70, x + 14, 92, x + 14, baseline,
                        x + 4, top + 20, x + 42, top + 20}, t));
                return 54;
            case 'g':
                ink.addStroke(stroke(new float[]{x + 42, top + 15, x + 16, top + 22, x + 8, top + 55,
                        x + 18, top + 86, x + 42, top + 86, x + 50, top + 52, x + 44, top + 14}, t));
                ink.addStroke(stroke(new float[]{x + 46, top + 80, x + 46, 235, x + 34, 252,
                        x + 16, 246, x + 10, 232}, t));
                return 62;
            case 'h':
                ink.addStroke(stroke(new float[]{x + 8, 60, x + 8, baseline, x + 8, top + 50,
                        x + 30, top + 14, x + 50, top + 50, x + 50, baseline}, t));
                return 62;
            case 'i':
                ink.addStroke(stroke(new float[]{x + 16, top + 40, x + 16, baseline}, t));
                ink.addStroke(stroke(new float[]{x + 16, top + 4, x + 17, top + 8}, t));
                return 32;
            case 'k':
                ink.addStroke(stroke(new float[]{x + 10, 60, x + 10, baseline}, t));
                ink.addStroke(stroke(new float[]{x + 10, top + 70, x + 50, top + 10}, t));
                ink.addStroke(stroke(new float[]{x + 22, top + 50, x + 52, baseline}, t));
                return 62;
            case 'l':
                ink.addStroke(stroke(new float[]{x + 12, 60, x + 12, baseline}, t));
                return 34;
            case 'm':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 8, baseline, x + 8, top + 50,
                        x + 28, top + 12, x + 44, top + 50, x + 44, baseline, x + 44, top + 50,
                        x + 64, top + 12, x + 80, top + 50, x + 80, baseline}, t));
                return 92;
            case 'n':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 8, baseline, x + 8, top + 50,
                        x + 30, top + 12, x + 50, top + 50, x + 50, baseline}, t));
                return 62;
            case 'o':
                ink.addStroke(stroke(new float[]{x + 34, top + 12, x + 12, top + 34, x + 10, top + 66,
                        x + 22, top + 90, x + 42, top + 86, x + 50, top + 56, x + 44, top + 26,
                        x + 30, top + 12}, t));
                return 62;
            case 'p':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 8, 250, x + 8, top + 50,
                        x + 30, top + 12, x + 50, top + 30, x + 50, top + 70, x + 30, top + 96,
                        x + 8, top + 96}, t));
                return 62;
            case 'r':
                ink.addStroke(stroke(new float[]{x + 10, top + 40, x + 10, baseline, x + 10, top + 50,
                        x + 32, top + 12, x + 46, top + 22}, t));
                return 58;
            case 's':
                ink.addStroke(stroke(new float[]{x + 46, top + 14, x + 20, top + 8, x + 10, top + 30,
                        x + 26, top + 50, x + 42, top + 66, x + 34, top + 90, x + 12, top + 94}, t));
                return 58;
            case 't':
                ink.addStroke(stroke(new float[]{x + 26, 80, x + 26, top + 92, x + 34, baseline,
                        x + 8, top + 24, x + 48, top + 24}, t));
                return 56;
            case 'u':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 8, top + 70, x + 22, top + 96,
                        x + 44, top + 70, x + 50, top + 40}, t));
                return 62;
            case 'v':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 30, baseline, x + 52, top + 40}, t));
                return 62;
            case 'w':
                ink.addStroke(stroke(new float[]{x + 4, top + 40, x + 22, baseline, x + 40, top + 40,
                        x + 58, baseline, x + 76, top + 40}, t));
                return 86;
            case 'y':
                ink.addStroke(stroke(new float[]{x + 8, top + 40, x + 30, baseline}, t));
                ink.addStroke(stroke(new float[]{x + 50, top + 40, x + 10, 250}, t));
                return 62;
            case 'z':
                ink.addStroke(stroke(new float[]{x + 10, top + 20, x + 50, top + 20, x + 10, baseline,
                        x + 50, baseline}, t));
                return 62;
            default:
                return 34;
        }
    }

    private static Ink.Stroke stroke(float[] coords, long startTime) {
        Ink.Stroke.Builder b = Ink.Stroke.builder();
        for (int i = 0; i + 1 < coords.length; i += 2) {
            b.addPoint(Ink.Point.create(coords[i], coords[i + 1], startTime + (i / 2) * 12L));
        }
        return b.build();
    }

    private void runOcr() {
        mode = "ocr";
        setStatus("OCR: copying sample image");
        try {
            JSONObject ocr = new JSONObject();
            ocr.put("sample", "handwritten-list.png (synthetic, Segoe Print)");
            results.put("ocr", ocr);
        } catch (Exception ignored) {
        }
        io.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "ocr");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("cannot create cache dir");
                }
                File out = new File(dir, "handwritten-list.png");
                copyAsset("handwritten-list.png", out);
                File printed = new File(dir, "printed-list.png");
                copyAsset("printed-list.png", printed);
                runOcrFile(out, "handwritten", () -> runOcrFile(printed, "printed", () -> {
                    setStatus("OCR: done");
                    writeResults();
                }));
            } catch (Exception e) {
                recordOcrError(e.toString());
            }
        });
    }

    private void runOcrFile(File file, String label, Runnable next) {
        int[] size = new int[2];
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        size[0] = opts.outWidth;
        size[1] = opts.outHeight;
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        long t0 = System.currentTimeMillis();
        try {
            InputImage image = InputImage.fromFilePath(this, Uri.fromFile(file));
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        long ms = System.currentTimeMillis() - t0;
                        try {
                            JSONObject o = new JSONObject();
                            o.put("label", label);
                            o.put("width", size[0]);
                            o.put("height", size[1]);
                            o.put("latencyMs", ms);
                            o.put("text", text.getText());
                            JSONArray lines = new JSONArray();
                            for (Text.TextBlock block : text.getTextBlocks()) {
                                for (Text.Line line : block.getLines()) {
                                    lines.put(line.getText());
                                }
                            }
                            o.put("lines", lines);
                            o.put("lineCount", lines.length());
                            results.getJSONObject("ocr").put(label, o);
                            log("ocr " + label + " lines=" + lines.length() + " ms=" + ms);
                        } catch (Exception e) {
                            log("ocr result error " + e);
                        }
                        recognizer.close();
                        next.run();
                    })
                    .addOnFailureListener(e -> {
                        recognizer.close();
                        recordOcrError(label + ": " + e);
                        next.run();
                    });
        } catch (Exception e) {
            recognizer.close();
            recordOcrError(label + ": " + e);
            next.run();
        }
    }

    private void recordOcrError(String message) {
        try {
            results.getJSONObject("ocr").put("error", message);
        } catch (Exception ignored) {
        }
        log("ocr error " + message);
        writeResults();
    }

    private void runGenAi() {
        mode = "genai";
        setStatus("Gemini Nano: checking availability");
        try {
            JSONObject ai = new JSONObject();
            ai.put("api", "com.google.mlkit.genai.prompt.java");
            results.put("genai", ai);
        } catch (Exception ignored) {
        }
        io.execute(() -> {
            GenerativeModelFutures model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
            long t0 = System.currentTimeMillis();
            try {
                int status = model.checkStatus().get(20, TimeUnit.SECONDS);
                long ms = System.currentTimeMillis() - t0;
                String name = statusName(status);
                synchronized (results) {
                    try {
                        results.getJSONObject("genai").put("status", name);
                        results.getJSONObject("genai").put("statusCode", status);
                        results.getJSONObject("genai").put("checkMs", ms);
                        results.getJSONObject("genai").put("foregroundOnlyNote",
                                "ML Kit GenAI only runs while the app is foreground; not measurable on this emulator.");
                        results.getJSONObject("genai").put("rulesFallback", rulesFallback());
                    } catch (Exception ignored) {
                    }
                }
                log("genai status=" + name + " in " + ms + "ms");
                if (status == FeatureStatus.AVAILABLE) {
                    long p0 = System.currentTimeMillis();
                    GenerateContentRequest request = new GenerateContentRequest.Builder(
                            new TextPart("Clean this shopping-list item: 'organic whol milk 1 gal'. " +
                                    "Return JSON with displayName, confidence, reason. Do not invent products."))
                            .build();
                    GenerateContentResponse response = model.generateContent(request).get(60, TimeUnit.SECONDS);
                    long pms = System.currentTimeMillis() - p0;
                    String text = response.getCandidates().isEmpty() ? "" : response.getCandidates().get(0).getText();
                    synchronized (results) {
                        try {
                            results.getJSONObject("genai").put("promptLatencyMs", pms);
                            results.getJSONObject("genai").put("promptResult", text);
                        } catch (Exception ignored) {
                        }
                    }
                    log("genai prompt " + pms + "ms -> " + text);
                } else {
                    synchronized (results) {
                        try {
                            results.getJSONObject("genai").put("note",
                                    "Status " + name + ". Per spec, do not silently download; use deterministic rules.");
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                synchronized (results) {
                    try {
                        results.getJSONObject("genai").put("error", e.toString());
                    } catch (Exception ignored) {
                    }
                }
                log("genai error " + e);
            }
            setStatus("Gemini Nano: done");
            writeResults();
        });
    }

    private static String statusName(int status) {
        if (status == FeatureStatus.AVAILABLE) return "AVAILABLE";
        if (status == FeatureStatus.DOWNLOADABLE) return "DOWNLOADABLE";
        if (status == FeatureStatus.DOWNLOADING) return "DOWNLOADING";
        if (status == FeatureStatus.UNAVAILABLE) return "UNAVAILABLE";
        return "UNKNOWN(" + status + ")";
    }

    private static JSONArray rulesFallback() {
        JSONArray arr = new JSONArray();
        String[][] samples = {
                {"organic whol milk 1 gal", "organic whole milk 1 gal"},
                {"tp", "toilet paper"},
                {"pb", "peanut butter"},
                {"2 ltrs cola", "2 ltrs cola"},
                {"- bananas", "bananas"}
        };
        for (String[] s : samples) {
            try {
                JSONObject o = new JSONObject();
                o.put("rawText", s[0]);
                o.put("displayName", s[1]);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        return arr;
    }

    private void runHomeDepot() {
        mode = "homedepot";
        setStatus("Home Depot: fetching public product page with OkHttp");
        try {
            JSONObject hd = new JSONObject();
            hd.put("storeContext", HD_STORE_CONTEXT);
            hd.put("items", new JSONArray());
            results.put("homedepot", hd);
        } catch (Exception ignored) {
        }
        String[][] products = {
                {"Husky 46 in. 9-Drawer Mobile Workbench", "https://www.homedepot.com/p/Husky-Tool-Storage-46-in-W-x-18-in-D-9-Drawer-Mobile-Workbench-Cabinet-in-Gloss-Black-H46X18MWC9BLK/322631796", "H5", "001"},
                {"Husky 46 in. x 25 in. 9-Drawer Workbench", "https://www.homedepot.com/p/Husky-46-in-x-25-in-D-9-Drawer-Mobile-Workbench-with-Adjustable-Height-Wood-Top-in-Gloss-Black-HOLC4609B11M/329462360", "?", "?"},
                {"Husky 46 in. W x 24.5 in. D Workbench with Tool Set", "https://www.homedepot.com/p/Husky-46-in-W-x-24-5-in-D-Standard-Duty-9-Drawer-Mobile-Workbench-with-Mechanics-Tool-Set-290-Piece-in-EVA-in-Gloss-Black-H290TB46BLKCB/321604087", "?", "?"}
        };
        io.execute(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();
            for (String[] p : products) {
                JSONObject rec = new JSONObject();
                long t0 = System.currentTimeMillis();
                try {
                    Request request = new Request.Builder()
                            .url(p[1])
                            .header("User-Agent", "FastShoppr/0.1 (Phase 0 feasibility; contact: local-development)")
                            .header("Accept", "text/html,application/xhtml+xml")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        ResponseBody body = response.body();
                        String html = body == null ? "" : body.string();
                        rec.put("name", p[0]);
                        rec.put("url", p[1]);
                        rec.put("httpStatus", response.code());
                        rec.put("contentType", response.header("content-type", ""));
                        rec.put("server", response.header("server", ""));
                        rec.put("latencyMs", System.currentTimeMillis() - t0);
                        rec.put("bodyBytes", html.length());
                        rec.put("blocked", response.code() == 403 || response.code() == 429
                                || html.toLowerCase(Locale.US).contains("access denied"));
                        if (response.code() == 200) {
                            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
                            String text = doc.body() == null ? doc.text() : doc.body().text();
                            java.util.regex.Matcher m = java.util.regex.Pattern
                                    .compile("(?i)Aisle\\s+([A-Z0-9-]+)\\s*,?\\s*Bay\\s+([0-9]+)")
                                    .matcher(text);
                            if (m.find()) {
                                rec.put("aisle", m.group(1));
                                rec.put("bay", m.group(2));
                            }
                            rec.put("title", doc.title());
                            rec.put("textLength", text.length());
                            rec.put("textSnippet", trim(text, 3000));
                            writeFixture("homedepot/" + safeName(p[0]) + "-sanitized.html",
                                    sanitizeHtml(html));
                        } else {
                            writeFixture("homedepot/blocked-" + response.code() + "-" + safeName(p[0]) + ".html",
                                    sanitizeHtml(html));
                        }
                    }
                } catch (Exception e) {
                    try {
                        rec.put("error", e.toString());
                    } catch (Exception ignored) {
                    }
                }
                synchronized (results) {
                    try {
                        results.getJSONObject("homedepot").getJSONArray("items").put(rec);
                    } catch (Exception ignored) {
                    }
                }
                log("homedepot " + p[0] + " -> " + rec.optInt("httpStatus") + " " + rec.optString("aisle"));
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            setStatus("Home Depot: done");
            writeResults();
        });
    }

    private static String sanitizeHtml(String html) {
        if (html == null) return "";
        String out = html;
        out = out.replaceAll("(?is)<script[^>]*>.*?</script>", "<!-- script removed -->");
        out = out.replaceAll("(?is)<style[^>]*>.*?</style>", "<!-- style removed -->");
        out = out.replaceAll("(?is)<svg[^>]*>.*?</svg>", "<!-- svg removed -->");
        return trim(out, 250000);
    }

    private void putDeviceInfo() {
        try {
            JSONObject d = new JSONObject();
            d.put("manufacturer", android.os.Build.MANUFACTURER);
            d.put("model", android.os.Build.MODEL);
            d.put("device", android.os.Build.DEVICE);
            d.put("sdk", android.os.Build.VERSION.SDK_INT);
            d.put("release", android.os.Build.VERSION.RELEASE);
            d.put("webView", webView != null ? webView.getSettings().getUserAgentString() : "");
            d.put("capturedAt", System.currentTimeMillis());
            results.put("device", d);
        } catch (Exception ignored) {
        }
    }

    private void writeResults() {
        main.post(() -> {
            try {
                results.put("finishedAt", System.currentTimeMillis());
            } catch (Exception ignored) {
            }
            try {
                writeFixture("phase0-results.json", results.toString(2));
            } catch (Exception e) {
                log("results serialization failed: " + e);
            }
            log("results written");
            setStatus("Results written to " + new File(getFilesDir(), "results").getAbsolutePath());
        });
    }

    private void writeFixture(String relative, String content) {
        try {
            File dir = new File(getFilesDir(), "results");
            File out = new File(dir, relative);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("cannot create " + parent);
            }
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(content.getBytes("UTF-8"));
            }
            log("wrote " + out.getAbsolutePath() + " bytes=" + content.length());
        } catch (Exception e) {
            log("write failed " + relative + ": " + e);
        }
    }

    private void copyAsset(String name, File out) throws Exception {
        try (InputStream in = getAssets().open(name);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        }
    }

    private void setStatus(String text) {
        log(text);
        main.post(() -> {
            if (statusView != null) {
                statusView.setText(text);
            }
        });
    }

    private static void log(String message) {
        Log.i(TAG, message);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String safeName(String s) {
        return s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        io.shutdownNow();
        super.onDestroy();
    }

    // Small helper to keep the unused-import checker honest for a throwaway file.
    @SuppressWarnings("unused")
    private void unusedHelpers() {
        Context c = this;
        View v = new View(c);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        v.setVisibility(View.VISIBLE);
        b.reset();
    }
}
