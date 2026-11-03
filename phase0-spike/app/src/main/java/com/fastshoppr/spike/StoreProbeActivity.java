package com.fastshoppr.spike;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class StoreProbeActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_LABEL = "label";
    private static final String TAG = "PHASE0";

    private WebView webView;
    private TextView result;
    private String label = "probe";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_probe);
        String url = getIntent().getStringExtra(EXTRA_URL);
        label = getIntent().getStringExtra(EXTRA_LABEL);
        if (label == null) label = "probe";

        ((TextView) findViewById(R.id.probeUrl)).setText(url);
        result = findViewById(R.id.extractResult);
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);

        ((Button) findViewById(R.id.btnExtract)).setOnClickListener(v -> extract());
    }

    private void extract() {
        String js =
            "(function(){" +
            "var r={};" +
            "r.title=document.title||'';" +
            "r.finalUrl=location.href;" +
            "var t=(document.body&&document.body.innerText)||'';" +
            "r.bodySample=t.substring(0,2000);" +
            "r.isRobotWall=/robot or human/i.test(t)||/robot or human/i.test(document.title||'');" +
            "r.hasNextData=(typeof window.__NEXT_DATA__!=='undefined');" +
            "var html=document.documentElement?document.documentElement.outerHTML:'';" +
            "r.htmlLength=html.length;" +
            "var m=html.match(/aisle/gi); r.aisleMentions=m?m.length:0;" +
            "var b=html.match(/bay/gi); r.bayMentions=b?b.length:0;" +
            "var loc=html.match(/(aisle|bay|location)[^<>]{0,80}\\d{1,3}/gi)||[];" +
            "r.locationSamples=loc.slice(0,15);" +
            "var prices=t.match(/\\$\\d+\\.\\d{2}/g)||[]; r.priceCount=prices.length; r.priceSamples=prices.slice(0,15);" +
            "var sizes=t.match(/\\d+\\s?(ct|sheets|rolls|pack|oz|lb|ft)\\b/gi)||[]; r.sizeSamples=sizes.slice(0,10);" +
            "r.hasStoreContext=/(pickup|deliver|ship).{0,40}(at|to)|store #?\\d+/i.test(t);" +
            "var nd=''; try { nd = window.__NEXT_DATA__ ? JSON.stringify(window.__NEXT_DATA__) : ''; } catch(e) {}" +
            "var hits=[]; var re=/(aisle|bay|storeId|storeName|location).{0,60}/gi; var mm;" +
            "while ((mm = re.exec(nd)) !== null && hits.length < 20) hits.push(mm[0]);" +
            "r.nextDataHits=hits;" +
            "var prods=Array.from(document.querySelectorAll('a[href*=\"/ip/\"],a[href*=\"/p/\"]')).slice(0,10).map(a=>a.getAttribute('href'));" +
            "r.productLinks=prods;" +
            "return JSON.stringify(r);" +
            "})()";
        webView.evaluateJavascript(js, value -> {
            String out = "LABEL: " + label + "\nRAW: " + value;
            result.setText(out);
            saveResult(label.replaceAll("[^a-zA-Z0-9_-]", "_") + ".txt", out);
            Log.i(TAG, "Extract[" + label + "] saved");
        });
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
        } catch (Exception e) {
            Log.e(TAG, "save failed", e);
        }
    }
}
