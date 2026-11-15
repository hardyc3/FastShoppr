/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class OrganizingActionReceiver extends BroadcastReceiver {

  @Inject OrganizingScheduler scheduler;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null || !OrganizingContracts.ACTION_CANCEL.equals(intent.getAction())) {
      return;
    }
    String listId = intent.getStringExtra(OrganizingContracts.EXTRA_LIST_ID);
    if (listId != null) {
      scheduler.cancel(listId);
    }
  }
}
