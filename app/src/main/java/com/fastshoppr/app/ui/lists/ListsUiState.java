/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import com.fastshoppr.app.ui.common.UiError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ListsUiState {

  private final boolean loading;
  private final List<ListRow> rows;
  private final boolean showArchived;
  private final boolean empty;
  private final UiError error;

  public ListsUiState(
      boolean loading, List<ListRow> rows, boolean showArchived, boolean empty, UiError error) {
    this.loading = loading;
    this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    this.showArchived = showArchived;
    this.empty = empty;
    this.error = error;
  }

  public static ListsUiState loading() {
    return new ListsUiState(true, List.of(), false, false, null);
  }

  public boolean isLoading() {
    return loading;
  }

  public List<ListRow> getRows() {
    return rows;
  }

  public boolean isShowArchived() {
    return showArchived;
  }

  public boolean isEmpty() {
    return empty;
  }

  public UiError getError() {
    return error;
  }
}
