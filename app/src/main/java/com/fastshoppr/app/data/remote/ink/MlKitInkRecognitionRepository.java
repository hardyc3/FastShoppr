/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.ink;

import com.fastshoppr.app.domain.model.InkData;
import com.fastshoppr.app.domain.model.InkPoint;
import com.fastshoppr.app.domain.model.InkStroke;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.common.RecognitionCandidate;
import com.google.mlkit.vision.digitalink.common.RecognitionResult;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.recognition.Ink;
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext;
import com.google.mlkit.vision.digitalink.recognition.WritingArea;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * ML Kit Digital Ink implementation. Strokes never leave the device; only the model download uses
 * the network.
 */
public final class MlKitInkRecognitionRepository implements InkRecognitionRepository {

  private static final String LANGUAGE_TAG = "en-US";

  private final Object lock = new Object();

  private DigitalInkRecognitionModel model;
  private DigitalInkRecognizer recognizer;
  private CompletableFuture<Void> pendingPreparation;

  @Inject
  public MlKitInkRecognitionRepository() {}

  private DigitalInkRecognitionModel requireModel() throws MlKitException {
    synchronized (lock) {
      if (model == null) {
        DigitalInkRecognitionModelIdentifier identifier =
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGUAGE_TAG);
        model = DigitalInkRecognitionModel.builder(identifier).build();
      }
      return model;
    }
  }

  @Override
  public void prepareModel(ModelListener listener) {
    try {
      prepare(requireModel(), listener);
    } catch (Exception exception) {
      listener.onError(exception);
    }
  }

  private void prepare(DigitalInkRecognitionModel remoteModel, ModelListener listener) {
    synchronized (lock) {
      if (pendingPreparation != null && !pendingPreparation.isDone()) {
        CompletableFuture<Void> inFlight = pendingPreparation;
        inFlight.whenComplete((ignored, error) -> notifyResult(listener, error));
        return;
      }
      pendingPreparation = new CompletableFuture<>();
      CompletableFuture<Void> preparation = pendingPreparation;
      RemoteModelManager manager = RemoteModelManager.getInstance();
      manager
          .isModelDownloaded(remoteModel)
          .addOnSuccessListener(
              downloaded -> {
                if (downloaded) {
                  preparation.complete(null);
                  notifyResult(listener, null);
                  return;
                }
                listener.onDownloading();
                manager
                    .download(remoteModel, new DownloadConditions.Builder().build())
                    .addOnSuccessListener(
                        ignored -> {
                          preparation.complete(null);
                          notifyResult(listener, null);
                        })
                    .addOnFailureListener(
                        error -> {
                          preparation.completeExceptionally(error);
                          notifyResult(listener, error);
                        });
              })
          .addOnFailureListener(
              error -> {
                preparation.completeExceptionally(error);
                notifyResult(listener, error);
              });
    }
  }

  private static void notifyResult(ModelListener listener, Object error) {
    if (error == null) {
      listener.onReady();
    } else if (error instanceof Exception exception) {
      listener.onError(exception);
    } else {
      listener.onError(new IllegalStateException(String.valueOf(error)));
    }
  }

  @Override
  public CompletableFuture<List<RecognizedLine>> recognize(InkData ink) {
    CompletableFuture<List<RecognizedLine>> future = new CompletableFuture<>();
    if (ink == null || ink.isEmpty()) {
      future.complete(List.of());
      return future;
    }
    try {
      requireModel();
      Ink mlInk = toMlInk(ink);
      RecognitionContext context =
          RecognitionContext.builder()
              .setWritingArea(
                  new WritingArea(
                      Math.max(ink.getCanvasWidth(), 1), Math.max(ink.getCanvasHeight(), 1)))
              .setPreContext("")
              .build();
      DigitalInkRecognizer activeRecognizer = recognizer();
      activeRecognizer
          .recognize(mlInk, context)
          .addOnSuccessListener(result -> future.complete(toLines(result)))
          .addOnFailureListener(future::completeExceptionally);
    } catch (Exception exception) {
      future.completeExceptionally(exception);
    }
    return future;
  }

  @Override
  public CompletableFuture<Void> deleteDownloadedModel() {
    CompletableFuture<Void> result = new CompletableFuture<>();
    try {
      close();
      RemoteModelManager.getInstance()
          .deleteDownloadedModel(requireModel())
          .addOnSuccessListener(ignored -> result.complete(null))
          .addOnFailureListener(result::completeExceptionally);
    } catch (Exception exception) {
      result.completeExceptionally(exception);
    }
    return result;
  }

  @Override
  public void close() {
    synchronized (lock) {
      if (recognizer != null) {
        recognizer.close();
        recognizer = null;
      }
    }
  }

  private DigitalInkRecognizer recognizer() throws MlKitException {
    synchronized (lock) {
      if (recognizer == null) {
        recognizer =
            DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(requireModel()).build());
      }
      return recognizer;
    }
  }

  private static Ink toMlInk(InkData ink) {
    Ink.Builder builder = Ink.builder();
    for (InkStroke stroke : ink.getStrokes()) {
      Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
      for (InkPoint point : stroke.getPoints()) {
        strokeBuilder.addPoint(
            Ink.Point.create(point.getX(), point.getY(), point.getTimestampMillis()));
      }
      builder.addStroke(strokeBuilder.build());
    }
    return builder.build();
  }

  private static List<RecognizedLine> toLines(RecognitionResult result) {
    List<RecognitionCandidate> candidates = result.getCandidates();
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    RecognitionCandidate best = candidates.get(0);
    String text = best.getText();
    if (text == null || text.isBlank()) {
      return List.of();
    }
    float confidence = confidenceOf(best, candidates);
    List<RecognizedLine> lines = new ArrayList<>();
    for (String line : text.split("\\r?\\n")) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        lines.add(new RecognizedLine(trimmed, confidence));
      }
    }
    return List.copyOf(lines);
  }

  private static float confidenceOf(
      RecognitionCandidate best, List<RecognitionCandidate> candidates) {
    if (best.getScore() != null) {
      return best.getScore();
    }
    float highest = Float.NEGATIVE_INFINITY;
    for (RecognitionCandidate candidate : candidates) {
      if (candidate.getScore() != null) {
        highest = Math.max(highest, candidate.getScore());
      }
    }
    return highest == Float.NEGATIVE_INFINITY ? RecognizedLine.UNKNOWN_CONFIDENCE : highest;
  }
}
