/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.content.Context;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.ExecutorService;

public final class OrganizingWorkerDeps {

  @EntryPoint
  @InstallIn(SingletonComponent.class)
  public interface Deps {
    ListItemRepository listItemRepository();

    StoreRepository storeRepository();

    AisleLookupRepository aisleLookupRepository();

    Clock clock();

    ExecutorService executor();

    NotificationHelper notificationHelper();

    OrganizingProgressStore progressStore();

    OrganizingScheduler scheduler();
  }

  public static Deps get(Context context) {
    return EntryPointAccessors.fromApplication(context.getApplicationContext(), Deps.class);
  }

  private OrganizingWorkerDeps() {}
}
