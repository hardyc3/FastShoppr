/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import com.fastshoppr.app.domain.model.RecognizedLine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import javax.inject.Inject;

/** JSON codec for {@code CaptureSession.recognizedLinesJson}. */
public final class RecognizedLinesCodec {

  private static final TypeToken<List<RecognizedLine>> LIST_TYPE = new TypeToken<>() {};

  private final Gson gson;

  @Inject
  public RecognizedLinesCodec(Gson gson) {
    this.gson = gson;
  }

  public String encode(List<RecognizedLine> lines) {
    return gson.toJson(lines == null ? List.of() : lines);
  }

  public List<RecognizedLine> decode(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<RecognizedLine> lines = gson.fromJson(json, LIST_TYPE.getType());
      return lines == null ? List.of() : List.copyOf(lines);
    } catch (RuntimeException exception) {
      return List.of();
    }
  }
}
