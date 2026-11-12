/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.text.TextNormalizer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

/** Validates model JSON before a suggestion can reach the review screen. */
public final class ItemNameOutputValidator {

  private static final Pattern URL_PATTERN = Pattern.compile("(?i)(https?://|www\\.)");
  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");

  private final Gson gson;

  @Inject
  public ItemNameOutputValidator(Gson gson) {
    this.gson = gson;
  }

  public CleanItemResult validate(String rawText, String modelOutput) {
    String json = extractJson(modelOutput);
    if (json == null) {
      return new CleanItemResult.Unavailable();
    }
    ModelResponse response;
    try {
      response = gson.fromJson(json, ModelResponse.class);
    } catch (JsonSyntaxException | IllegalStateException exception) {
      return new CleanItemResult.Unavailable();
    }
    if (response == null || response.displayName == null) {
      return new CleanItemResult.Unavailable();
    }
    String displayName = TextNormalizer.collapseWhitespace(response.displayName);
    if (displayName == null || displayName.isBlank()) {
      return new CleanItemResult.Unavailable();
    }
    if (displayName.length() > 200) {
      return new CleanItemResult.Unavailable();
    }
    if (URL_PATTERN.matcher(displayName).find()) {
      return new CleanItemResult.Unavailable();
    }
    if (!sameNumbers(rawText, displayName)) {
      return new CleanItemResult.Unavailable();
    }
    if (TextNormalizer.collapseWhitespace(rawText).equalsIgnoreCase(displayName)) {
      return new CleanItemResult.NoChange();
    }
    float confidence = response.confidence == null ? 0f : response.confidence;
    if (!Float.isFinite(confidence)) {
      confidence = 0f;
    }
    confidence = Math.max(0f, Math.min(1f, confidence));
    String reason = TextNormalizer.trimToNull(response.reason);
    if (reason != null && reason.length() > 200) {
      reason = reason.substring(0, 200);
    }
    return new CleanItemResult.Suggested(displayName, confidence, reason);
  }

  static List<String> numericTokens(String value) {
    List<String> tokens = new ArrayList<>();
    if (value == null) {
      return tokens;
    }
    Matcher matcher = NUMBER_PATTERN.matcher(value);
    while (matcher.find()) {
      tokens.add(matcher.group().replace(',', '.'));
    }
    Collections.sort(tokens);
    return tokens;
  }

  private static boolean sameNumbers(String rawText, String displayName) {
    return numericTokens(rawText).equals(numericTokens(displayName));
  }

  private static String extractJson(String output) {
    if (output == null) {
      return null;
    }
    String trimmed = output.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start < 0 || end <= start) {
      return null;
    }
    return trimmed.substring(start, end + 1);
  }

  private static final class ModelResponse {
    private String displayName;
    private Float confidence;
    private String reason;
  }
}
