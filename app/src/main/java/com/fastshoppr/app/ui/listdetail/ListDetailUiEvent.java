/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;

public sealed interface ListDetailUiEvent
    permits ListDetailUiEvent.ShowError, ListDetailUiEvent.ShowMessage {

  record ShowError(UiError error) implements ListDetailUiEvent {}

  record ShowMessage(UiMessage message) implements ListDetailUiEvent {}
}
