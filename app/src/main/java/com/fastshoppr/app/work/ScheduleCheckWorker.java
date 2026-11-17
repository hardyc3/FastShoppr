/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class ScheduleCheckWorker extends Worker {

  public ScheduleCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      ScheduleWorkerDeps.get(getApplicationContext())
          .createScheduledListUseCase()
          .runIfDue()
          .join();
      return Result.success();
    } catch (Exception e) {
      return Result.retry();
    }
  }
}
