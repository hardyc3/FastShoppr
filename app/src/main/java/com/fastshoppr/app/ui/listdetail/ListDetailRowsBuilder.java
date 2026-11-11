/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.sort.AisleComparator;
import com.fastshoppr.app.domain.usecase.SortItemsUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Builds the store-section rows shown by {@link ListDetailAdapter}. */
public final class ListDetailRowsBuilder {

  private final SortItemsUseCase sortItemsUseCase;

  public ListDetailRowsBuilder(SortItemsUseCase sortItemsUseCase) {
    this.sortItemsUseCase = sortItemsUseCase;
  }

  public List<ListDetailRow> build(
      List<Store> orderedStores,
      List<ListItem> items,
      SortDirection direction,
      Function<String, String> draftForStore,
      long addGeneration) {
    List<ListDetailRow> rows = new ArrayList<>();
    for (Store store : orderedStores) {
      List<ListItem> storeItems = itemsForStore(items, store);
      List<ListItem> sorted = sortItemsUseCase.sort(storeItems, direction);
      rows.add(new ListDetailRow.StoreHeader(store, sorted.size()));

      List<ListItem> resolved = new ArrayList<>();
      List<ListItem> unresolved = new ArrayList<>();
      for (ListItem item : sorted) {
        if (AisleComparator.isUnresolved(item.getAisle())) {
          unresolved.add(item);
        } else {
          resolved.add(item);
        }
      }
      resolved.forEach(item -> rows.add(new ListDetailRow.Item(item)));
      if (!unresolved.isEmpty()) {
        rows.add(new ListDetailRow.AttentionHeader(store.getId()));
        unresolved.forEach(item -> rows.add(new ListDetailRow.Item(item)));
      }
      rows.add(new ListDetailRow.AddItem(store, draftForStore.apply(store.getId()), addGeneration));
    }
    return List.copyOf(rows);
  }

  private static List<ListItem> itemsForStore(List<ListItem> items, Store store) {
    List<ListItem> storeItems = new ArrayList<>();
    for (ListItem item : items) {
      if (item.getStoreId().equals(store.getId())) {
        storeItems.add(item);
      }
    }
    return storeItems;
  }
}
