/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.RecognizedLine;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Digital ink recognition with model management. Implementations must keep strokes on device and
 * never upload them.
 */
public interface InkRecognitionRepository {

  /** Progress callbacks for the English model check/download. */
  interface ModelListener {
    void onDownloading();

    void onReady();

    void onError(Exception error);
  }

  /** Checks model availability and downloads the English model on first use. */
  void prepareModel(ModelListener listener);

  /** Recognizes ink on device. Completes with an empty list when nothing is recognized. */
  CompletableFuture<List<RecognizedLine>> recognize(InkData ink);

  /** Releases the cached recognizer. Safe to call more than once. */
  void close();
}
