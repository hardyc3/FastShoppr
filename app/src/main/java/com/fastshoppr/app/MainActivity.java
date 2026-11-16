/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.fastshoppr.app.databinding.ActivityMainBinding;
import com.fastshoppr.app.work.ScheduleWorkerScheduler;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity {

  @Inject ScheduleWorkerScheduler scheduleWorkerScheduler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    applySystemBarInsets(binding.getRoot());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (scheduleWorkerScheduler != null) {
      scheduleWorkerScheduler.checkOnAppOpen();
    }
  }

  /** Keeps app content clear of the status bar, navigation bar, and the software keyboard. */
  private static void applySystemBarInsets(View root) {
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, windowInsets) -> {
          Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
          view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
          return windowInsets;
        });
  }
}
