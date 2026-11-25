/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.content.Context;
import com.fastshoppr.app.domain.usecase.CleanUpExpiredDataUseCase;
import com.fastshoppr.app.domain.usecase.CreateScheduledListUseCase;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

public final class ScheduleWorkerDeps {

  @EntryPoint
  @InstallIn(SingletonComponent.class)
  public interface Deps {
    CreateScheduledListUseCase createScheduledListUseCase();

    CleanUpExpiredDataUseCase cleanUpExpiredDataUseCase();

    ScheduleWorkerScheduler scheduleWorkerScheduler();
  }

  public static Deps get(Context context) {
    return EntryPointAccessors.fromApplication(context.getApplicationContext(), Deps.class);
  }

  private ScheduleWorkerDeps() {}
}
