/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class CameraCaptureViewModel extends ViewModel {

  private final Context context;
  private Object textRecognizer;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private CameraCaptureViewModel(Context context) {
    this.context = context;
    this.textRecognizer = null;
  }

  public static CameraCaptureViewModel getInstance(Context context) {
    return new CameraCaptureViewModel(context);
  }

  public void ensureTextRecognizer(Runnable callback) {
    mainHandler.postDelayed(callback, 100);
  }

  public void recognizeImage(Bitmap bitmap, int rotation, Runnable callback) {
    mainHandler.postDelayed(() -> callback.run(), 100);
  }

  public void reset() {
    textRecognizer = null;
  }

  @Override
  protected void onCleared() {
    textRecognizer = null;
  }

  public static class Factory implements ViewModelProvider.Factory {

    private final Context context;

    public Factory(Context context) {
      this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(Class<T> modelClass) {
      return (T) new CameraCaptureViewModel(context);
    }
  }
}
