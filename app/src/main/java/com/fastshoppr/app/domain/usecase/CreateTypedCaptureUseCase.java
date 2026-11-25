/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/** Creates a capture session from keyboard input, one trimmed item per line. */
public final class CreateTypedCaptureUseCase {

  private final CaptureRepository captureRepository;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public CreateTypedCaptureUseCase(
      CaptureRepository captureRepository,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor) {
    this.captureRepository = captureRepository;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
  }

  /** Persists a typed capture and returns its real capture id. */
  public CompletableFuture<String> create(String listId, String typedText) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<CapturedLine> lines = draftLines(typedText);
          if (lines.isEmpty()) {
            throw new IllegalArgumentException("typed text must not be blank");
          }
          String json = captureRepository.encodeDraft(lines);
          CaptureSession session =
              new CaptureSession(
                  idGenerator.newId(),
                  listId,
                  ItemSource.TYPED,
                  null,
                  json,
                  clock.nowMillis(),
                  null,
                  null);
          captureRepository.insert(session);
          return session.getId();
        },
        executor);
  }

  private static List<CapturedLine> draftLines(String typedText) {
    List<CapturedLine> lines = new ArrayList<>();
    if (typedText == null) {
      return lines;
    }
    for (String line : typedText.split("\\R")) {
      String text = line.trim();
      if (text.isEmpty()) {
        continue;
      }
      lines.add(new CapturedLine(text, null, true, 0.0f, true, null, null));
    }
    return lines;
  }
}
