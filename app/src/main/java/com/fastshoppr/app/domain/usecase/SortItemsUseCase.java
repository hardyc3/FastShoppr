/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.sort.ListItemComparator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public final class SortItemsUseCase {

  @Inject
  public SortItemsUseCase() {}

  public List<ListItem> sort(List<ListItem> items, SortDirection direction) {
    List<ListItem> sorted = new ArrayList<>(items);
    sorted.sort(new ListItemComparator(direction));
    return List.copyOf(sorted);
  }
}
