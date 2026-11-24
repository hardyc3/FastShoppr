/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * JSON codec for the review draft stored in {@code CaptureSession.recognizedLinesJson}. Reads the
 * legacy recognized-line format so existing sessions remain valid.
 */
public final class CapturedLinesCodec {

  private final Gson gson;

  @Inject
  public CapturedLinesCodec(Gson gson) {
    this.gson = gson;
  }

  public String encode(List<CapturedLine> lines) {
    return gson.toJson(lines == null ? List.of() : lines);
  }

  public List<CapturedLine> decode(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    JsonElement root;
    try {
      root = JsonParser.parseString(json);
    } catch (RuntimeException malformed) {
      return List.of();
    }
    if (!root.isJsonArray()) {
      return List.of();
    }
    List<CapturedLine> lines = new ArrayList<>();
    for (JsonElement element : root.getAsJsonArray()) {
      CapturedLine line = decodeLine(element);
      if (line != null) {
        lines.add(line);
      }
    }
    return List.copyOf(lines);
  }

  private static CapturedLine decodeLine(JsonElement element) {
    if (element == null || !element.isJsonObject()) {
      return null;
    }
    JsonObject object = element.getAsJsonObject();
    String text = string(object, "text");
    if (text.isEmpty()) {
      return null;
    }
    return new CapturedLine(
        text,
        nullable(object, "displayName"),
        !object.has("selected") || object.get("selected").getAsBoolean(),
        number(object, "confidence", RecognizedLine.UNKNOWN_CONFIDENCE),
        object.has("manual") && object.get("manual").getAsBoolean(),
        nullable(object, "storeId"),
        nullable(object, "targetDate"));
  }

  private static String string(JsonObject object, String key) {
    String value = nullable(object, key);
    return value == null ? "" : value;
  }

  private static String nullable(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  private static float number(JsonObject object, String key, float fallback) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? fallback : value.getAsFloat();
  }
}
