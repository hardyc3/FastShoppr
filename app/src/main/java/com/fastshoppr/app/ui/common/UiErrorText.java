/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import androidx.annotation.StringRes;
import com.fastshoppr.app.R;

public final class UiErrorText {

  private UiErrorText() {}

  @StringRes
  public static int stringRes(UiError error) {
    return switch (error) {
      case LOAD_FAILED -> R.string.lists_load_failed;
      case CREATE_FAILED -> R.string.list_create_failed;
      case LIST_NOT_FOUND -> R.string.list_not_found;
      case ITEM_NOT_FOUND -> R.string.item_not_found;
      case ADD_FAILED -> R.string.item_add_failed;
      case SAVE_FAILED -> R.string.item_save_failed;
      case DELETE_FAILED -> R.string.item_delete_failed;
      case DEFER_FAILED -> R.string.item_defer_failed;
      case ARCHIVE_FAILED -> R.string.list_archive_failed;
      case RAW_TEXT_REQUIRED -> R.string.raw_text_required;
      case RAW_TEXT_TOO_LONG -> R.string.raw_text_too_long;
      case RECOGNITION_FAILED -> R.string.recognition_failed;
      case INK_MODEL_NOT_READY -> R.string.ink_model_not_ready;
      case CAPTURE_NOT_FOUND -> R.string.capture_not_found;
      case NO_STORES -> R.string.no_stores;
      case NO_LINES_SELECTED -> R.string.review_no_lines_selected;
    };
  }
}
