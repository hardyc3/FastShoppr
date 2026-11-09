/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

public final class SingleEvent<T> {

  private final T content;
  private boolean handled;

  public SingleEvent(T content) {
    this.content = content;
  }

  public T getContentIfNotHandled() {
    if (handled) {
      return null;
    }
    handled = true;
    return content;
  }
}
