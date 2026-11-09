/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.text;

import android.content.Context;
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
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * ML Kit Text Recognition v2 (Latin) implementation for camera and gallery images. Runs fully on
 * device; images are never uploaded or logged.
 */
@Singleton
public final class MlKitImageTextRecognitionRepository implements ImageTextRecognitionRepository {

  private static final int MAX_IMAGE_DIMENSION = 2048;

  private final Context appContext;
  private final Object lock = new Object();

  private TextRecognizer recognizer;

  @Inject
  public MlKitImageTextRecognitionRepository(@ApplicationContext Context appContext) {
    this.appContext = appContext;
  }

  @Override
  public CompletableFuture<List<RecognizedLine>> recognize(String imagePath) {
    CompletableFuture<List<RecognizedLine>> future = new CompletableFuture<>();
    if (imagePath == null || imagePath.isBlank()) {
      future.complete(List.of());
      return future;
    }
    try {
      InputImage image = loadDownscaled(new File(imagePath));
      recognizer()
          .process(image)
          .addOnSuccessListener(text -> future.complete(toLines(text)))
          .addOnFailureListener(future::completeExceptionally);
    } catch (IOException | RuntimeException exception) {
      future.completeExceptionally(exception);
    }
    return future;
  }

  /** Releases the cached recognizer. Safe to call more than once. */
  public void close() {
    synchronized (lock) {
      if (recognizer != null) {
        recognizer.close();
        recognizer = null;
      }
    }
  }

  private TextRecognizer recognizer() {
    synchronized (lock) {
      if (recognizer == null) {
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
      }
      return recognizer;
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
