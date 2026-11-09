/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;

public sealed interface ListsUiEvent
    permits ListsUiEvent.OpenList, ListsUiEvent.ShowError, ListsUiEvent.ShowMessage {

  record OpenList(String listId) implements ListsUiEvent {}

  record ShowError(UiError error) implements ListsUiEvent {}

  record ShowMessage(UiMessage message) implements ListsUiEvent {}
}
