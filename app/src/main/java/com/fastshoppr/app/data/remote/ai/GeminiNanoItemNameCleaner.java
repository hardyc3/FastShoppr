/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.ai;

import com.fastshoppr.app.domain.ai.CleanItemPromptBuilder;
import com.fastshoppr.app.domain.ai.ItemNameCleaner;
import com.fastshoppr.app.domain.ai.ItemNameOutputValidator;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.Candidate;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.SystemInstruction;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gemini Nano implementation of {@link ItemNameCleaner}. All ML Kit calls run on the injected
 * background executor; the model is only used while a foreground screen owns the caller.
 */
@Singleton
public final class GeminiNanoItemNameCleaner implements ItemNameCleaner {

  private static final long STATUS_TIMEOUT_SECONDS = 20;
  private static final long GENERATE_TIMEOUT_SECONDS = 60;
  private static final int MAX_OUTPUT_TOKENS = 256;

  private final ExecutorService executor;
  private final ItemNameOutputValidator validator;
  private final AtomicReference<GenerativeModelFutures> model = new AtomicReference<>();
  private final Set<CompletableFuture<?>> inFlight = ConcurrentHashMap.newKeySet();

  @Inject
  public GeminiNanoItemNameCleaner(ExecutorService executor, ItemNameOutputValidator validator) {
    this.executor = executor;
    this.validator = validator;
  }

  @Override
  public CompletableFuture<AiAvailability> availability() {
    return track(
        CompletableFuture.supplyAsync(
            () -> {
              try {
                int status = model().checkStatus().get(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return mapStatus(status);
              } catch (ExecutionException exception) {
                return mapError(exception.getCause());
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return new AiAvailability.Unavailable();
              } catch (Exception exception) {
                return new AiAvailability.Unavailable();
              }
            },
            executor));
  }

  /** Cancellation drops the result; an already started ML Kit call is bounded by its timeout. */
  @Override
  public CompletableFuture<CleanItemResult> clean(CleanItemRequest request) {
    CompletableFuture<CleanItemResult> result = new CompletableFuture<>();
    track(result);
    executor.execute(
        () -> {
          if (result.isCancelled()) {
            return;
          }
          try {
            GenerateContentRequest.Builder requestBuilder =
                new GenerateContentRequest.Builder(
                    new SystemInstruction(CleanItemPromptBuilder.SYSTEM_INSTRUCTION),
                    new TextPart(CleanItemPromptBuilder.buildUserPrompt(request)));
            requestBuilder.setTemperature(0f);
            requestBuilder.setMaxOutputTokens(MAX_OUTPUT_TOKENS);
            GenerateContentResponse response =
                model()
                    .generateContent(requestBuilder.build())
                    .get(GENERATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result.isCancelled()) {
              return;
            }
            List<Candidate> candidates = response.getCandidates();
            if (candidates.isEmpty() || candidates.get(0).getText() == null) {
              result.complete(new CleanItemResult.Unavailable());
              return;
            }
            result.complete(validator.validate(request.getRawText(), candidates.get(0).getText()));
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.complete(new CleanItemResult.Unavailable());
          } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            result.complete(new CleanItemResult.Unavailable());
          }
        });
    return result;
  }

  @Override
  public CompletableFuture<Void> downloadModel(Consumer<Long> progressBytes) {
    return track(
        CompletableFuture.runAsync(
            () -> {
              try {
                DownloadCallback callback =
                    new DownloadCallback() {
                      @Override
                      public void onDownloadProgress(long totalBytesDownloaded) {
                        if (progressBytes != null) {
                          progressBytes.accept(totalBytesDownloaded);
                        }
                      }
                    };
                model().download(callback).get();
              } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(exception);
              } catch (ExecutionException exception) {
                throw new CompletionException(
                    exception.getCause() == null ? exception : exception.getCause());
              }
            },
            executor));
  }

  private <T> CompletableFuture<T> track(CompletableFuture<T> future) {
    inFlight.add(future);
    future.whenComplete((result, error) -> inFlight.remove(future));
    return future;
  }

  private GenerativeModelFutures model() {
    GenerativeModelFutures current = model.get();
    if (current != null) {
      return current;
    }
    GenerativeModelFutures created = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
    if (model.compareAndSet(null, created)) {
      return created;
    }
    return model.get();
  }

  private static AiAvailability mapStatus(int status) {
    if (status == FeatureStatus.AVAILABLE) {
      return new AiAvailability.Available();
    }
    if (status == FeatureStatus.DOWNLOADABLE) {
      return new AiAvailability.Downloadable();
    }
    if (status == FeatureStatus.DOWNLOADING) {
      return new AiAvailability.Downloading();
    }
    return new AiAvailability.Unavailable();
  }

  private static AiAvailability mapError(Throwable error) {
    if (error instanceof GenAiException genAiException) {
      int code = genAiException.getErrorCode();
      if (code == GenAiException.ErrorCode.BUSY
          || code == GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED) {
        return new AiAvailability.QuotaExceeded();
      }
    }
    return new AiAvailability.Unavailable();
  }

  /** Cancels in-flight inference and drops the cached model reference. */
  @Override
  public void close() {
    for (CompletableFuture<?> future : inFlight) {
      future.cancel(true);
    }
    inFlight.clear();
    model.set(null);
  }
}
