/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.RecognizedLine;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** On-device text recognition for camera and gallery images. */
public interface ImageTextRecognitionRepository {

  /**
   * Runs text recognition on the image at the given app-private path. Completes with an empty list
   * when no text is recognized.
   */
  CompletableFuture<List<RecognizedLine>> recognize(String imagePath);
}
