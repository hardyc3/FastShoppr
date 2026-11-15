/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.ui.common.UiError;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ListDetailUiState {

  private final boolean loading;
  private final String listName;
  private final LocalDate shoppingDate;
  private final List<ListDetailRow> rows;
  private final List<Store> stores;
  private final SortDirection sortDirection;
  private final UiError error;

  public ListDetailUiState(
      boolean loading,
      String listName,
      LocalDate shoppingDate,
      List<ListDetailRow> rows,
      List<Store> stores,
      SortDirection sortDirection,
      UiError error) {
    this.loading = loading;
    this.listName = listName;
    this.shoppingDate = shoppingDate;
    this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    this.stores = Collections.unmodifiableList(new ArrayList<>(stores));
    this.sortDirection = sortDirection;
    this.error = error;
  }

  public static ListDetailUiState loading() {
    return new ListDetailUiState(true, null, null, List.of(), List.of(), SortDirection.ASC, null);
  }

  public boolean isLoading() {
    return loading;
  }

  public String getListName() {
    return listName;
  }

  public LocalDate getShoppingDate() {
    return shoppingDate;
  }

  public List<ListDetailRow> getRows() {
    return rows;
  }

  public List<Store> getStores() {
    return stores;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  public UiError getError() {
    return error;
  }
}
