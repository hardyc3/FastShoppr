/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.fastshoppr.app.domain.model.ShoppingList;
import java.time.LocalDate;
import java.util.List;

public interface ShoppingListRepository {

  LiveData<List<ShoppingList>> observeActive();

  LiveData<List<ShoppingList>> observeArchived();

  List<ShoppingList> getActive();

  ShoppingList getById(String id);

  ShoppingList getByIdIncludingDeleted(String id);

  ShoppingList getOrCreate(
      LocalDate shoppingDate, String name, String scheduleId, boolean isAutoCreated);

  void update(ShoppingList list);

  void archive(String id);

  void restore(String id);

  void softDelete(String id);

  int purgeDeletedBefore(long cutoffMillis);
}
