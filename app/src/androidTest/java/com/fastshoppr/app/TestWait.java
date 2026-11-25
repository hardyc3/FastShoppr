/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import android.os.SystemClock;

/** Shared polling helper for instrumentation tests. */
public final class TestWait {

  private TestWait() {}

  public static void waitFor(Runnable assertion) {
    waitFor(assertion, 5_000L);
  }

  public static void waitFor(Runnable assertion, long timeoutMillis) {
    long deadline = SystemClock.uptimeMillis() + timeoutMillis;
    while (SystemClock.uptimeMillis() < deadline) {
      try {
        assertion.run();
        return;
      } catch (AssertionError | RuntimeException exception) {
        SystemClock.sleep(50L);
      }
    }
    assertion.run();
  }
}
