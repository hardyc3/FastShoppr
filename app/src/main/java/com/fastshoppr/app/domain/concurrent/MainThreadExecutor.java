/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.concurrent;

/** Runs work on the UI thread without exposing Android framework types to callers. */
@FunctionalInterface
public interface MainThreadExecutor {

  void execute(Runnable action);
}
