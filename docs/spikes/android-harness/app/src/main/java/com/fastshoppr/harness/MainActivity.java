package com.fastshoppr.harness;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.walmart_button)
                .setOnClickListener(v -> startActivity(new Intent(this, WalmartProbeActivity.class)));
        findViewById(R.id.ink_button)
                .setOnClickListener(v -> startActivity(new Intent(this, DigitalInkProbeActivity.class)));
        findViewById(R.id.text_button)
                .setOnClickListener(v -> startActivity(new Intent(this, TextRecognitionProbeActivity.class)));
        findViewById(R.id.gemini_button)
                .setOnClickListener(v -> startActivity(new Intent(this, GeminiProbeActivity.class)));
    }
}
