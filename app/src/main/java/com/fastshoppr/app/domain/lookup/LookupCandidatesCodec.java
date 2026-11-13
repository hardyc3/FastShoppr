/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.lookup;

import com.fastshoppr.app.domain.model.LookupSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializes candidate matches from a {@code Multiple} result so they survive process death and can
 * be offered from the item row. Raw candidates carry retailer-provided text as untrusted data; they
 * are displayed with normal Gson decoding and never executed.
 */
public final class LookupCandidatesCodec {

  private static final Gson GSON = new Gson();
  private static final TypeToken<List<Candidate>> LIST_TYPE = new TypeToken<List<Candidate>>() {};

  private LookupCandidatesCodec() {}

  /** Encoded, persisted, round-trippable shape. */
  private static final class Candidate {
    String aisle;
    String bay;
    String productName;
    String productUrl;
    Float confidence;
    String source;
  }

  public static String encode(List<StoreLookupResult.Found> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    List<Candidate> encoded = new ArrayList<>();
    for (StoreLookupResult.Found f : candidates) {
      Candidate c = new Candidate();
      c.aisle = f.getAisle();
      c.bay = f.getBay();
      c.productName = f.getProductName();
      c.productUrl = f.getProductUrl();
      c.confidence = f.getConfidence();
      c.source = f.getSource().name();
      encoded.add(c);
    }
    return GSON.toJson(encoded, LIST_TYPE.getType());
  }

  /**
   * @return an unmodifiable candidate list, or an empty list when missing/unreadable
   */
  public static List<StoreLookupResult.Found> decode(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      List<Candidate> decoded = GSON.fromJson(json, LIST_TYPE.getType());
      if (decoded == null) {
        return Collections.emptyList();
      }
      List<StoreLookupResult.Found> out = new ArrayList<>();
      for (Candidate c : decoded) {
        if (c == null || c.aisle == null || c.aisle.isBlank()) {
          continue;
        }
        if (c.source == null) {
          continue;
        }
        LookupSource source;
        try {
          source = LookupSource.valueOf(c.source);
        } catch (IllegalArgumentException ignored) {
          continue;
        }
        out.add(
            new StoreLookupResult.Found(
                c.aisle, c.bay, c.productName, c.productUrl, c.confidence, source));
      }
      return Collections.unmodifiableList(out);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
