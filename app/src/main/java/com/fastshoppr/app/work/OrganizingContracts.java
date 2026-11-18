/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

public final class OrganizingContracts {

  public static final String CHANNEL_ORGANIZING = "organizing";
  public static final String KEY_LIST_ID = "list_id";
  public static final String KEY_FORCE_REFRESH = "force_refresh";

  public static final String ACTION_CANCEL = "com.fastshoppr.app.action.CANCEL_ORGANIZING";
  public static final String ACTION_CANCEL_ALL = "com.fastshoppr.app.action.CANCEL_ALL_ORGANIZING";
  public static final String ACTION_WALMART_PROGRESS = "com.fastshoppr.app.action.WALMART_PROGRESS";

  public static final String EXTRA_LIST_ID = "extra_list_id";
  public static final String EXTRA_DONE = "extra_done";
  public static final String EXTRA_TOTAL = "extra_total";
  public static final String EXTRA_STATUS = "extra_status";
  public static final String EXTRA_WALMART_FOREGROUND_NEEDED = "extra_walmart_fg_needed";

  public static String organizeWorkName(String listId) {
    return "organize_list_" + listId;
  }

  public static String homeDepotWorkName(String listId) {
    return "lookup_homedepot_" + listId;
  }

  private OrganizingContracts() {}
}
