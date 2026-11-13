/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.error;

public sealed interface AppError
    permits AppError.NetworkUnavailable,
        AppError.RateLimited,
        AppError.AccessBlocked,
        AppError.ParserFailed,
        AppError.LookupNotFound,
        AppError.AiUnavailable,
        AppError.UnknownError {

  record NetworkUnavailable(String message) implements AppError {
    public NetworkUnavailable() {
      this("");
    }
  }

  record RateLimited(String message) implements AppError {
    public RateLimited() {
      this("");
    }
  }

  record AccessBlocked(String message) implements AppError {
    public AccessBlocked() {
      this("");
    }
  }

  record ParserFailed(String message) implements AppError {
    public ParserFailed() {
      this("");
    }
  }

  record LookupNotFound(String message) implements AppError {
    public LookupNotFound() {
      this("");
    }
  }

  record AiUnavailable(String message) implements AppError {
    public AiUnavailable() {
      this("");
    }
  }

  record UnknownError(String message) implements AppError {
    public UnknownError() {
      this("");
    }
  }
}
