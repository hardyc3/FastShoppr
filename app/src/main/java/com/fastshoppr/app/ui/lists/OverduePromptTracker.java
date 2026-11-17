/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class OverduePromptTracker {
  private final Set<String> prompted = Collections.synchronizedSet(new HashSet<>());

  @Inject
  public OverduePromptTracker() {}

  public boolean shouldPrompt(String listId) {
    return listId != null && !prompted.contains(listId);
  }

  public void markPrompted(String listId) {
    if (listId != null) prompted.add(listId);
  }
}
