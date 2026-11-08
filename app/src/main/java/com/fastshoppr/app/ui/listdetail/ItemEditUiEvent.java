/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.ui.common.UiError;

public sealed interface ItemEditUiEvent permits ItemEditUiEvent.Saved, ItemEditUiEvent.ShowError {

  record Saved() implements ItemEditUiEvent {}

  record ShowError(UiError error) implements ItemEditUiEvent {}
}
