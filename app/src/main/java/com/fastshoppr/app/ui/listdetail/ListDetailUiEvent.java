/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiMessage;

public sealed interface ListDetailUiEvent
    permits ListDetailUiEvent.ShowError,
        ListDetailUiEvent.ShowMessage,
        ListDetailUiEvent.RequestDeferral,
        ListDetailUiEvent.NeedsScheduleDate,
        ListDetailUiEvent.NavigateToBranchPicker {

  record ShowError(UiError error) implements ListDetailUiEvent {}

  record ShowMessage(UiMessage message) implements ListDetailUiEvent {}

  record RequestDeferral(
      String storeId,
      String originalText,
      String itemTextWithoutPhrase,
      String phrase,
      java.time.LocalDate resolvedDate,
      java.time.LocalDate targetListDate,
      String existingTargetListId,
      boolean needsConfirmation,
      String note)
      implements ListDetailUiEvent {}

  record NeedsScheduleDate(
      String storeId, String originalText, String itemTextWithoutPhrase, String phrase)
      implements ListDetailUiEvent {}

  record NavigateToBranchPicker(StoreBrand brand) implements ListDetailUiEvent {}
}
