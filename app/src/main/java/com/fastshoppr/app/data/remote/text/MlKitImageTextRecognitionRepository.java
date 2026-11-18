/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.text;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.exifinterface.media.ExifInterface;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.ImageTextRecognitionRepository;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * ML Kit Text Recognition v2 (Latin) implementation for camera and gallery images. Runs fully on
 * device; images are never uploaded or logged.
 */
public final class MlKitImageTextRecognitionRepository implements ImageTextRecognitionRepository {

  private static final int MAX_IMAGE_DIMENSION = 2048;

  private final ExecutorService executor;
  private final Object lock = new Object();

  private TextRecognizer recognizer;
  private int inFlight;
  private boolean closing;

  @Inject
  public MlKitImageTextRecognitionRepository(ExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public CompletableFuture<List<RecognizedLine>> recognize(String imagePath) {
    CompletableFuture<List<RecognizedLine>> future = new CompletableFuture<>();
    if (imagePath == null || imagePath.isBlank()) {
      future.complete(List.of());
      return future;
    }
    // Decoding and EXIF reads are disk work and can take tens of milliseconds for camera photos.
    executor.execute(
        () -> {
          try {
            InputImage image = loadDownscaled(new File(imagePath));
            startRecognition(image, future);
          } catch (IOException | RuntimeException exception) {
            future.completeExceptionally(exception);
          }
        });
    return future;
  }

  @Override
  public void close() {
    synchronized (lock) {
      closing = true;
      if (inFlight == 0) {
        closeRecognizerLocked();
        closing = false;
      }
    }
  }

  private void startRecognition(InputImage image, CompletableFuture<List<RecognizedLine>> future) {
    synchronized (lock) {
      if (closing) {
        future.completeExceptionally(
            new IllegalStateException("Text recognition is closing; retry the capture."));
        return;
      }
      if (recognizer == null) {
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
      }
      inFlight++;
      try {
        recognizer
            .process(image)
            .addOnSuccessListener(
                text -> {
                  future.complete(toLines(text));
                  recognitionFinished();
                })
            .addOnFailureListener(
                error -> {
                  future.completeExceptionally(error);
                  recognitionFinished();
                });
      } catch (RuntimeException error) {
        inFlight--;
        throw error;
      }
    }
  }

  private void recognitionFinished() {
    synchronized (lock) {
      inFlight--;
      if (closing && inFlight == 0) {
        closeRecognizerLocked();
        closing = false;
      }
    }
  }

  private void closeRecognizerLocked() {
    if (recognizer != null) {
      recognizer.close();
      recognizer = null;
    }
  }

  /**
   * Decodes the image down to at most {@link #MAX_IMAGE_DIMENSION} on its longest side and applies
   * EXIF rotation so recognition sees the upright image.
   */
  private InputImage loadDownscaled(File file) throws IOException {
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
      throw new IOException("unsupported image: " + file.getName());
    }
    int sampleSize = 1;
    int longestSide = Math.max(bounds.outWidth, bounds.outHeight);
    while (longestSide / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
      sampleSize *= 2;
    }
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = sampleSize;
    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    if (bitmap == null) {
      throw new IOException("could not decode image: " + file.getName());
    }
    int rotationDegrees = rotationFromExif(file);
    return InputImage.fromBitmap(bitmap, rotationDegrees);
  }

  private static int rotationFromExif(File file) {
    try {
      ExifInterface exif = new ExifInterface(file.getAbsolutePath());
      int orientation =
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      return switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
        case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
        case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
        default -> 0;
      };
    } catch (IOException exception) {
      return 0;
    }
  }

  private static List<RecognizedLine> toLines(Text text) {
    List<RecognizedLine> lines = new ArrayList<>();
    for (Text.TextBlock block : text.getTextBlocks()) {
      for (Text.Line line : block.getLines()) {
        String value = line.getText();
        if (value == null || value.isBlank()) {
          continue;
        }
        lines.add(new RecognizedLine(value.trim(), lineConfidence(line)));
      }
    }
    return List.copyOf(lines);
  }

  private static float lineConfidence(Text.Line line) {
    List<Text.Element> elements = line.getElements();
    if (elements.isEmpty()) {
      return RecognizedLine.UNKNOWN_CONFIDENCE;
    }
    float sum = 0.0f;
    for (Text.Element element : elements) {
      sum += element.getConfidence();
    }
    return sum / elements.size();
  }
}
