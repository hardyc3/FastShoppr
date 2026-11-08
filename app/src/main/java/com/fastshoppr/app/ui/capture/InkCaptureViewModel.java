/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class InkCaptureViewModel extends ViewModel {

  private final InkCanvasView inkCanvas;
  private Object recognizer;
  private Object recognitionContext;
  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private InkCaptureViewModel(InkCanvasView inkCanvas, Context context) {
    this.inkCanvas = inkCanvas;
    this.context = context;
    this.recognizer = null;
    this.recognitionContext = null;
  }

  public static InkCaptureViewModel getInstance(InkCanvasView inkCanvas, Context context) {
    return new InkCaptureViewModel(inkCanvas, context);
  }

  public void ensureRecognizer(Runnable callback) {
    mainHandler.postDelayed(callback, 100);
  }

  public void recognizeCurrent(Runnable callback) {
    mainHandler.postDelayed(callback, 100);
  }

  public void resetStrokes() {
    inkCanvas.invalidate();
  }

  public void clear() {
    recognizer = null;
    recognitionContext = null;
    resetStrokes();
  }

  @Override
  protected void onCleared() {
    recognizer = null;
    recognitionContext = null;
  }

  public static class Factory implements ViewModelProvider.Factory {

    private final InkCanvasView inkCanvas;
    private final Context context;

    public Factory(InkCanvasView inkCanvas, Context context) {
      this.inkCanvas = inkCanvas;
      this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(Class<T> modelClass) {
      return (T) new InkCaptureViewModel(inkCanvas, context);
    }
  }
}
