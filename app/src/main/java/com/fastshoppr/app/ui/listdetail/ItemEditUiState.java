/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.ui.common.UiError;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemEditUiState {

  private final boolean loading;
  private final String rawText;
  private final String displayName;
  private final String quantity;
  private final String aisle;
  private final String bay;
  private final LocalDate targetDate;
  private final List<Store> stores;
  private final String selectedStoreId;
  private final String selectedStoreName;
  private final UiError error;

  public ItemEditUiState(
      boolean loading,
      String rawText,
      String displayName,
      String quantity,
      String aisle,
      String bay,
      LocalDate targetDate,
      List<Store> stores,
      String selectedStoreId,
      String selectedStoreName,
      UiError error) {
    this.loading = loading;
    this.rawText = rawText;
    this.displayName = displayName;
    this.quantity = quantity;
    this.aisle = aisle;
    this.bay = bay;
    this.targetDate = targetDate;
    this.stores = Collections.unmodifiableList(new ArrayList<>(stores));
    this.selectedStoreId = selectedStoreId;
    this.selectedStoreName = selectedStoreName;
    this.error = error;
  }

  public static ItemEditUiState loading() {
    return new ItemEditUiState(
        true, null, null, null, null, null, null, List.of(), null, null, null);
  }

  public boolean isLoading() {
    return loading;
  }

  public String getRawText() {
    return rawText;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getQuantity() {
    return quantity;
  }

  public String getAisle() {
    return aisle;
  }

  public String getBay() {
    return bay;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public List<Store> getStores() {
    return stores;
  }

  public String getSelectedStoreId() {
    return selectedStoreId;
  }

  public String getSelectedStoreName() {
    return selectedStoreName;
  }

  public UiError getError() {
    return error;
  }
}
