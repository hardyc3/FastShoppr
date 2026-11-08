/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import androidx.annotation.StringRes;
import com.fastshoppr.app.R;

public final class UiMessageText {

  private UiMessageText() {}

  @StringRes
  public static int stringRes(UiMessage message) {
    return switch (message) {
      case ITEM_ADDED -> R.string.item_added_message;
      case ITEM_DEFERRED -> R.string.item_deferred_message;
      case ITEM_DELETED -> R.string.item_deleted_message;
      case ITEM_SAVED -> R.string.item_saved_message;
      case LIST_ARCHIVED -> R.string.list_archived_message;
    };
  }
}
