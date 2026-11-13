/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.lookup;

import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.model.LookupSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Result of a store lookup attempt. */
public abstract class StoreLookupResult {

  private StoreLookupResult() {}

  public static final class Found extends StoreLookupResult {
    private final String aisle;
    private final String bay;
    private final String productName;
    private final String productUrl;
    private final Float confidence;
    private final LookupSource source;

    public Found(
        String aisle,
        String bay,
        String productName,
        String productUrl,
        Float confidence,
        LookupSource source) {
      this.aisle = aisle;
      this.bay = bay;
      this.productName = productName;
      this.productUrl = productUrl;
      this.confidence = confidence;
      this.source = Objects.requireNonNull(source, "source must not be null");
    }

    public String getAisle() {
      return aisle;
    }

    public String getBay() {
      return bay;
    }

    public String getProductName() {
      return productName;
    }

    public String getProductUrl() {
      return productUrl;
    }

    public Float getConfidence() {
      return confidence;
    }

    public LookupSource getSource() {
      return source;
    }
  }

  public static final class Multiple extends StoreLookupResult {
    private final List<Found> candidates;

    public Multiple(List<Found> candidates) {
      if (candidates == null || candidates.isEmpty()) {
        throw new IllegalArgumentException("candidates must not be empty");
      }
      this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    public List<Found> getCandidates() {
      return candidates;
    }
  }

  public static final class NotFound extends StoreLookupResult {
    private final String reason;

    public NotFound(String reason) {
      this.reason = reason == null ? "" : reason.trim();
    }

    public String getReason() {
      return reason;
    }
  }

  public static final class Blocked extends StoreLookupResult {
    private final String reason;

    public Blocked(String reason) {
      this.reason = reason == null ? "" : reason.trim();
    }

    public String getReason() {
      return reason;
    }
  }

  public static final class Error extends StoreLookupResult {
    private final AppError error;

    public Error(AppError error) {
      this.error = Objects.requireNonNull(error, "error must not be null");
    }

    public AppError getError() {
      return error;
    }
  }
}
