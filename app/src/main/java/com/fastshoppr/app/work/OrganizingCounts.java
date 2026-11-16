/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import java.util.List;

public final class OrganizingCounts {

  public final int found;
  public final int needsReview;
  public final int notFound;
  public final int errors;
  public final int pending;

  public OrganizingCounts(int found, int needsReview, int notFound, int errors, int pending) {
    this.found = found;
    this.needsReview = needsReview;
    this.notFound = notFound;
    this.errors = errors;
    this.pending = pending;
  }

  public static OrganizingCounts from(List<ListItem> items) {
    int found = 0;
    int needsReview = 0;
    int notFound = 0;
    int errors = 0;
    int pending = 0;
    if (items != null) {
      for (ListItem item : items) {
        if (item == null || item.getLookupStatus() == null) {
          continue;
        }
        LookupStatus s = item.getLookupStatus();
        if (s == LookupStatus.FOUND) found++;
        else if (s == LookupStatus.NEEDS_REVIEW) needsReview++;
        else if (s == LookupStatus.NOT_FOUND) notFound++;
        else if (s == LookupStatus.ERROR) errors++;
        else if (s == LookupStatus.PENDING
            || s == LookupStatus.QUEUED
            || s == LookupStatus.IN_PROGRESS) pending++;
      }
    }
    return new OrganizingCounts(found, needsReview, notFound, errors, pending);
  }

  public String summary() {
    return "Found "
        + found
        + ", needs review "
        + needsReview
        + ", not found "
        + notFound
        + ", errors "
        + errors;
  }
}
