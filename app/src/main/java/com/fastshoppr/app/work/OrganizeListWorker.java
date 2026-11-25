/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class OrganizeListWorker extends Worker {

  public OrganizeListWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    String listId = getInputData().getString(OrganizingContracts.KEY_LIST_ID);
    if (listId == null || listId.isBlank()) {
      return Result.failure();
    }
    OrganizingWorkerDeps.Deps deps = OrganizingWorkerDeps.get(getApplicationContext());
    ListItemRepository itemRepo = deps.listItemRepository();
    StoreRepository storeRepo = deps.storeRepository();
    AisleLookupRepository lookupRepo = deps.aisleLookupRepository();
    Clock clock = deps.clock();
    NotificationHelper notifications = deps.notificationHelper();
    OrganizingProgressStore progressStore = deps.progressStore();

    try {
      List<ListItem> all = itemRepo.getByList(listId);
      List<ListItem> eligible = new ArrayList<>();
      for (ListItem item : all) {
        if (item == null || item.isDeleted()) continue;
        if (item.getLookupSource() == LookupSource.MANUAL) continue; // never overwrite manual
        LookupStatus s = item.getLookupStatus();
        if (s == LookupStatus.PENDING
            || s == LookupStatus.QUEUED
            || s == LookupStatus.IN_PROGRESS) {
          eligible.add(item);
        }
        if (s == LookupStatus.ERROR
            || s == LookupStatus.NOT_FOUND
            || s == LookupStatus.NEEDS_REVIEW) {
          // retry only if we want; for now include if user pressed retry (already set to QUEUED)
          if (s == LookupStatus.QUEUED) eligible.add(item);
        }
      }
      // Recompute eligible from latest: include queued only
      eligible.clear();
      for (ListItem item : all) {
        if (item == null || item.isDeleted()) continue;
        if (item.getLookupSource() == LookupSource.MANUAL) continue;
        if (item.getLookupStatus() == LookupStatus.QUEUED
            || item.getLookupStatus() == LookupStatus.PENDING
            || item.getLookupStatus() == LookupStatus.IN_PROGRESS) {
          eligible.add(item);
        }
      }

      if (eligible.isEmpty()) {
        List<ListItem> latest = itemRepo.getByList(listId);
        OrganizingCounts counts = OrganizingCounts.from(latest);
        notifications.notifyCompletion(listId, counts.summary());
        progressStore.clear(listId);
        return Result.success();
      }

      // Mark queued items as queued (idempotent) already done by scheduler; mark in-progress
      // handled per item
      List<ListItem> walmartItems = new ArrayList<>();
      List<ListItem> homeDepotItems = new ArrayList<>();
      for (ListItem item : eligible) {
        Store store = null;
        try {
          if (item.getStoreId() != null) store = storeRepo.getById(item.getStoreId());
        } catch (Exception ignored) {
        }
        if (store != null && store.getBrand() == StoreBrand.WALMART) {
          walmartItems.add(item);
        } else if (store != null && store.getBrand() == StoreBrand.HOME_DEPOT) {
          homeDepotItems.add(item);
        } else {
          // unsupported for auto-lookup: mark needs review/message
        }
      }

      int total = eligible.size();
      int done = 0;
      progressStore.update(
          new OrganizingProgressStore.Progress(listId, done, total, "Starting…", true, false));
      notifications.notifyProgress(
          listId,
          getApplicationContext().getString(R.string.notification_organizing_title),
          done,
          total,
          false);

      // Process Home Depot items first (HTTP, can run in WorkManager)
      boolean blocked = false;
      for (ListItem item : homeDepotItems) {
        try {
          ListItem cur = itemRepo.getById(item.getId());
          if (cur == null || cur.isDeleted() || LookupSource.MANUAL.equals(cur.getLookupSource())) {
            done++;
            continue;
          }
          ListItem ip =
              ListItem.builderFrom(cur)
                  .lookupStatus(LookupStatus.IN_PROGRESS)
                  .lookupError(null)
                  .updatedAt(clock.nowMillis())
                  .build();
          itemRepo.update(ip);
        } catch (Exception ignored) {
        }
        AisleLookupRepository.LookupOutcome outcome = null;
        try {
          outcome = lookupRepo.lookupItem(item.getId(), false);
        } catch (Exception ignored) {
        }
        done++;
        String status =
            blocked
                ? "Stopped (blocked)"
                : getApplicationContext()
                    .getString(R.string.notification_organizing_progress, done, total);
        progressStore.update(
            new OrganizingProgressStore.Progress(listId, done, total, status, true, false));
        notifications.notifyProgress(
            listId,
            getApplicationContext().getString(R.string.notification_organizing_title),
            done,
            total,
            false);
        if (outcome != null && outcome.blocked) {
          blocked = true;
          // Stop run on blocked; leave remaining items queued for later user action.
          break;
        }
      }

      // Handle Walmart items
      if (!walmartItems.isEmpty() && !blocked) {
        boolean permOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          permOk =
              ContextCompat.checkSelfPermission(
                      getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                  == PackageManager.PERMISSION_GRANTED;
        }
        if (!permOk) {
          // queue for foreground; keep Walmart items queued and inform user in-app via progress
          progressStore.update(
              new OrganizingProgressStore.Progress(
                  listId,
                  done,
                  total,
                  getApplicationContext().getString(R.string.lookup_walmart_queued_foreground),
                  false,
                  true));
          notifications.cancel(listId);
          // Schedule? no: items remain QUEUED until user opens app/organizes with permission.
        } else {
          try {
            Intent svc = new Intent(getApplicationContext(), WalmartLookupForegroundService.class);
            svc.putExtra(OrganizingContracts.EXTRA_LIST_ID, listId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              getApplicationContext().startForegroundService(svc);
            } else {
              getApplicationContext().startService(svc);
            }
            progressStore.update(
                new OrganizingProgressStore.Progress(
                    listId, done, total, "Walmart lookup running…", true, true));
          } catch (Exception e) {
            // FGS start failed: leave Walmart items queued and show in-app message
            progressStore.update(
                new OrganizingProgressStore.Progress(
                    listId,
                    done,
                    total,
                    getApplicationContext().getString(R.string.lookup_walmart_queued_foreground),
                    false,
                    true));
            notifications.cancel(listId);
          }
        }
      } else {
        // finish if no walmart work delegated to service
        List<ListItem> latest = itemRepo.getByList(listId);
        OrganizingCounts counts = OrganizingCounts.from(latest);
        notifications.notifyCompletion(listId, counts.summary());
        progressStore.update(
            new OrganizingProgressStore.Progress(
                listId, total, total, counts.summary(), false, false));
        progressStore.clear(listId);
      }

      // If items remain pending/queued (e.g. added during run, blocked, or Walmart queued for
      // foreground), ensure a follow-up run can pick them up when conditions allow.
      try {
        List<ListItem> latest = itemRepo.getByList(listId);
        boolean hasPending = false;
        for (ListItem item : latest) {
          if (item == null || item.isDeleted()) continue;
          LookupStatus s = item.getLookupStatus();
          if ((s == LookupStatus.PENDING || s == LookupStatus.QUEUED)
              && item.getLookupSource() != LookupSource.MANUAL) {
            hasPending = true;
            break;
          }
        }
        if (hasPending && (walmartItems.isEmpty() || blocked)) {
          deps.scheduler().scheduleAfterSave(listId);
        }
      } catch (Exception ignored) {
      }

      return Result.success();
    } catch (Exception e) {
      return Result.retry();
    }
  }
}
