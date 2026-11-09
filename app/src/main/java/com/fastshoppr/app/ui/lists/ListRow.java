/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import com.fastshoppr.app.domain.model.ShoppingListSummary;

public sealed interface ListRow permits ListRow.SectionHeader, ListRow.ListEntry {

  enum Section {
    UPCOMING,
    PAST,
    ARCHIVED
  }

  record SectionHeader(Section section) implements ListRow {}

  record ListEntry(ShoppingListSummary summary) implements ListRow {}
}
