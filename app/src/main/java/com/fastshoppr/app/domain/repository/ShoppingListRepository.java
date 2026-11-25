/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.domain.observation.Observable;
import java.time.LocalDate;
import java.util.List;

public interface ShoppingListRepository {

  Observable<List<ShoppingList>> observeActive();

  Observable<List<ShoppingListSummary>> observeActiveSummaries();

  Observable<List<ShoppingList>> observeArchived();

  Observable<List<ShoppingListSummary>> observeArchivedSummaries();

  Observable<List<ShoppingListSummary>> observeActiveSummariesInRange(
      LocalDate startInclusive, LocalDate endInclusive);

  Observable<ShoppingList> observeById(String id);

  List<ShoppingList> getActive();

  List<ShoppingListSummary> getActiveSummaries();

  List<ShoppingList> getPastListsWithUncheckedItems(LocalDate today);

  ShoppingList findNearestActiveOnOrAfter(LocalDate date);

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
