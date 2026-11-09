/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.navigation;

import android.os.Bundle;

public final class NavArguments {

  public static final String LIST_ID = "listId";
  public static final String CAPTURE_ID = "captureId";
  public static final String ITEM_ID = "itemId";
  public static final String LAUNCH_GALLERY = "launchGallery";

  public static final String PLACEHOLDER_LIST_ID = "placeholder-list";
  public static final String PLACEHOLDER_CAPTURE_ID = "placeholder-capture";

  private NavArguments() {}

  public static Bundle forList(String listId) {
    Bundle bundle = new Bundle();
    bundle.putString(LIST_ID, listId);
    return bundle;
  }

  public static Bundle forListLaunchGallery(String listId) {
    Bundle bundle = forList(listId);
    bundle.putBoolean(LAUNCH_GALLERY, true);
    return bundle;
  }

  public static Bundle forCapture(String captureId) {
    Bundle bundle = new Bundle();
    bundle.putString(CAPTURE_ID, captureId);
    return bundle;
  }

  public static Bundle forItem(String itemId) {
    Bundle bundle = new Bundle();
    bundle.putString(ITEM_ID, itemId);
    return bundle;
  }
}
