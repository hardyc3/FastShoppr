/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.AisleCache;
import com.fastshoppr.app.domain.model.ListItem;

/** Cache-first lookup orchestration for a single item. */
public interface AisleLookupRepository {

  LookupOutcome lookupItem(String itemId, boolean forceRefresh);

  /**
   * Applies a user-confirmed candidate selected from a {@code Multiple} result. Enforces the
   * manual-override invariant: a value the user entered manually is never overwritten.
   *
   * @return the updated item, or the unchanged item when the item is manual or missing
   */
  ListItem applyCandidate(String itemId, StoreLookupResult.Found candidate);

  final class LookupOutcome {
    public final ListItem item;
    public final AisleCache cacheEntry; // may be stale when offline/expired
    public final boolean fromCache;
    public final boolean stale;
    public final boolean blocked;
    public final StoreLookupResult result;

    public LookupOutcome(
        ListItem item,
        AisleCache cacheEntry,
        boolean fromCache,
        boolean stale,
        boolean blocked,
        StoreLookupResult result) {
      this.item = item;
      this.cacheEntry = cacheEntry;
      this.fromCache = fromCache;
      this.stale = stale;
      this.blocked = blocked;
      this.result = result;
    }
  }
}
