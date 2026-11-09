/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.fastshoppr.app.domain.model.ListItem;
import java.time.LocalDate;
import java.util.List;

public interface ListItemRepository {

  LiveData<List<ListItem>> observeByList(String listId);

  LiveData<List<ListItem>> observeByListAndStore(String listId, String storeId);

  LiveData<ListItem> observeById(String id);

  List<ListItem> getByList(String listId);

  ListItem getById(String id);

  int nextSortPosition(String listId, String storeId);

  void insert(ListItem item);

  void update(ListItem item);

  void setChecked(String id, boolean checked);

  void softDelete(String id);

  ListItem moveToDate(String itemId, LocalDate targetDate, String targetListName);

  int purgeDeletedBefore(long cutoffMillis);
}
