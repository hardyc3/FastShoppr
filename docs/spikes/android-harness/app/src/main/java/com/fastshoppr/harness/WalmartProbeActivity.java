package com.fastshoppr.harness;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

public final class WalmartProbeActivity extends Activity {

    private static final String DEFAULT_URL = "https://www.walmart.com/search?q=toilet+paper";

    private static final String EXTRACT_JS =
            """
            (function() {
              var visible = document.body ? document.body.innerText : '';
              var html = document.documentElement ? document.documentElement.outerHTML : '';
              var haystack = (visible + ' ' + document.title).toLowerCase();
              var blocked = /robot or human|access denied|captcha|unusual traffic|verify you are human/.test(haystack);

              var aisleMatches = [];
              var aisleRe = /aisle[^<>{}"']{0,80}/ig;
              var match;
              var count = 0;
              while ((match = aisleRe.exec(html)) !== null && count < 25) {
                aisleMatches.push(match[0].replace(/\\s+/g, ' ').trim());
                count++;
              }

              var scriptMatches = [];
              var scripts = document.querySelectorAll('script');
              for (var i = 0; i < scripts.length && scriptMatches.length < 10; i++) {
                var text = scripts[i].textContent || '';
                if (!/aisle|storeId|fulfillment|price/i.test(text)) {
                  continue;
                }
                var contexts = [];
                var contextRe = /.{0,60}(aisle|storeId|fulfillment|price).{0,100}/ig;
                var contextMatch;
                var contextCount = 0;
                while ((contextMatch = contextRe.exec(text)) !== null && contextCount < 5) {
                  contexts.push(contextMatch[0]);
                  contextCount++;
                }
                scriptMatches.push({
                  id: scripts[i].id || '',
                  type: scripts[i].type || '',
                  length: text.length,
                  contexts: contexts
                });
              }

              return JSON.stringify({
                url: location.href,
                title: document.title,
                blocked: blocked,
                aisleMatches: aisleMatches,
                scriptMatches: scriptMatches,
                visibleSample: visible.slice(0, 2500)
              }, null, 2);
            })()
            """;

    private EditText urlInput;
    private TextView statusText;
    private TextView resultText;
    private WebView webView;
    private Button extractButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walmart_probe);

        urlInput = findViewById(R.id.url_input);
        statusText = findViewById(R.id.status_text);
        resultText = findViewById(R.id.result_text);
        webView = findViewById(R.id.web_view);
        extractButton = findViewById(R.id.extract_button);

        urlInput.setText(DEFAULT_URL);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(
                settings.getUserAgentString() + " FastShopprPhase0Harness/0.1");

        webView.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        String host = uri.getHost();
                        return host == null || !host.endsWith("walmart.com");
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        extractButton.setEnabled(true);
                        statusText.setText("Page loaded. Select a store, wait for results, then Extract.");
                    }
                });

        findViewById(R.id.load_button)
                .setOnClickListener(
                        v -> {
                            String url = urlInput.getText().toString().trim();
                            Uri uri = Uri.parse(url);
                            String host = uri.getHost();
                            if (!"https".equals(uri.getScheme())
                                    || host == null
                                    || !host.endsWith("walmart.com")) {
                                statusText.setText("Only https://*.walmart.com URLs are allowed.");
                                return;
                            }
                            extractButton.setEnabled(false);
                            statusText.setText("Loading...");
                            resultText.setText("");
                            webView.loadUrl(url);
                        });

        extractButton.setEnabled(false);
        extractButton.setOnClickListener(v -> extract());
    }

    private void extract() {
        statusText.setText("Extracting...");
        webView.evaluateJavascript(
                EXTRACT_JS,
                value -> {
                    String decoded = decodeJavascriptString(value);
                    if (decoded == null) {
                        resultText.setText("Extraction returned no value.");
                        statusText.setText("Extraction failed.");
                        return;
                    }
                    resultText.setText(decoded);
                    statusText.setText(
                            decoded.contains("\"blocked\": true")
                                    ? "BLOCKED page detected. Stop the run and surface Blocked."
                                    : "Extraction complete. Inspect aisleMatches and scriptMatches.");
                });
    }

    private static String decodeJavascriptString(String value) {
        if (value == null || "null".equals(value)) {
            return null;
        }
        try {
            return new JSONArray("[" + value + "]").getString(0);
        } catch (JSONException e) {
            return value;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
