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

/** Daily retention work: expired aisle cache plus soft-deleted rows older than 30 days. */
public final class MaintenanceWorker extends Worker {

  public MaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      ScheduleWorkerDeps.get(getApplicationContext()).cleanUpExpiredDataUseCase().run().join();
      return Result.success();
    } catch (Exception e) {
      return Result.retry();
    }
  }
}
