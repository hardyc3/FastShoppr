/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

@AndroidEntryPoint
public class WalmartLookupForegroundService extends Service {

  @Inject ListItemRepository itemRepository;
  @Inject StoreRepository storeRepository;
  @Inject AisleLookupRepository lookupRepository;
  @Inject NotificationHelper notificationHelper;
  @Inject OrganizingProgressStore progressStore;
  @Inject ExecutorService executor;

  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private volatile String currentListId;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && OrganizingContracts.ACTION_CANCEL.equals(intent.getAction())) {
      String listId = intent.getStringExtra(OrganizingContracts.EXTRA_LIST_ID);
      if (listId != null && listId.equals(currentListId)) {
        cancelled.set(true);
      }
      stopForeground(STOP_FOREGROUND_REMOVE);
      stopSelf();
      return START_NOT_STICKY;
    }

    String listId =
        intent == null ? null : intent.getStringExtra(OrganizingContracts.EXTRA_LIST_ID);
    if (listId == null) {
      stopSelf();
      return START_NOT_STICKY;
    }
    currentListId = listId;
    cancelled.set(false);

    Notification notification =
        notificationHelper.buildProgress(
            listId, getString(R.string.notification_organizing_walmart_title), 0, 0, true, true);
    try {
      startForeground(notificationHelper.notificationId(listId), notification);
    } catch (Exception e) {
      // cannot start foreground service (restrictions); leave items queued
      progressStore.update(
          new OrganizingProgressStore.Progress(
              listId, 0, 0, getString(R.string.lookup_walmart_queued_foreground), false, true));
      stopSelf();
      return START_NOT_STICKY;
    }

    executor.execute(() -> run(listId));
    return START_NOT_STICKY;
  }

  private void run(String listId) {
    boolean wasCancelled = false;
    try {
      List<ListItem> all = itemRepository.getByList(listId);
      List<ListItem> walmart = new ArrayList<>();
      for (ListItem item : all) {
        if (item == null || item.isDeleted()) continue;
        if (LookupSource.MANUAL.equals(item.getLookupSource())) continue;
        LookupStatus s = item.getLookupStatus();
        if (s != LookupStatus.QUEUED
            && s != LookupStatus.PENDING
            && s != LookupStatus.IN_PROGRESS) {
          continue;
        }
        Store store = null;
        try {
          if (item.getStoreId() != null) store = storeRepository.getById(item.getStoreId());
        } catch (Exception ignored) {
        }
        if (store != null && store.getBrand() == StoreBrand.WALMART) {
          walmart.add(item);
        }
      }

      int total = walmart.size();
      int done = 0;
      if (total == 0) {
        finish(listId);
        return;
      }

      for (ListItem item : walmart) {
        if (cancelled.get() || stopped.get()) {
          wasCancelled = true;
          break;
        }
        ListItem cur = null;
        try {
          cur = itemRepository.getById(item.getId());
        } catch (Exception ignored) {
        }
        if (cur == null || cur.isDeleted() || LookupSource.MANUAL.equals(cur.getLookupSource())) {
          done++;
          continue;
        }
        if (cur.getLookupStatus() != LookupStatus.IN_PROGRESS) {
          try {
            ListItem ip =
                com.fastshoppr.app.domain.model.ListItem.builderFrom(cur)
                    .lookupStatus(LookupStatus.IN_PROGRESS)
                    .lookupError(null)
                    .updatedAt(System.currentTimeMillis())
                    .build();
            itemRepository.update(ip);
          } catch (Exception ignored) {
          }
        }
        updateProgress(listId, done, total);
        try {
          AisleLookupRepository.LookupOutcome outcome =
              lookupRepository.lookupItem(item.getId(), false);
          if (outcome != null && outcome.blocked) {
            // stop run on blocked; keep remaining items actionable (queued/pending) for later
            wasCancelled = cancelled.get();
            break;
          }
        } catch (Exception ignored) {
        }
        done++;
        updateProgress(listId, done, total);
      }

      if (cancelled.get() || wasCancelled) {
        handleCancelled(listId);
      } else {
        finish(listId);
      }
    } catch (Exception ignored) {
    } finally {
      stopForeground(STOP_FOREGROUND_REMOVE);
      stopSelf();
    }
  }

  private void handleCancelled(String listId) {
    try {
      long now = System.currentTimeMillis();
      List<ListItem> latest = itemRepository.getByList(listId);
      for (ListItem item : latest) {
        if (item == null || item.isDeleted()) continue;
        if (LookupSource.MANUAL.equals(item.getLookupSource())) continue;
        LookupStatus s = item.getLookupStatus();
        if (s == LookupStatus.IN_PROGRESS || s == LookupStatus.QUEUED) {
          try {
            ListItem updated =
                com.fastshoppr.app.domain.model.ListItem.builderFrom(item)
                    .lookupStatus(LookupStatus.PENDING)
                    .updatedAt(now)
                    .build();
            itemRepository.update(updated);
          } catch (Exception ignored) {
          }
        }
      }
      List<ListItem> after = itemRepository.getByList(listId);
      OrganizingCounts counts = OrganizingCounts.from(after);
      notificationHelper.notifyCompletion(listId, counts.summary());
      progressStore.update(
          new OrganizingProgressStore.Progress(
              listId, after.size(), after.size(), counts.summary(), false, false));
      progressStore.clear(listId);
    } catch (Exception ignored) {
      notificationHelper.cancel(listId);
      progressStore.clear(listId);
    }
  }

  private void updateProgress(String listId, int done, int total) {
    String status =
        getString(R.string.notification_organizing_progress, Math.max(0, done), Math.max(1, total));
    progressStore.update(
        new OrganizingProgressStore.Progress(listId, done, total, status, true, false));
    try {
      startForeground(
          notificationHelper.notificationId(listId),
          notificationHelper.buildProgress(
              listId,
              getString(R.string.notification_organizing_walmart_title),
              done,
              total,
              false,
              true));
    } catch (Exception ignored) {
    }
  }

  private void finish(String listId) {
    try {
      List<ListItem> latest = itemRepository.getByList(listId);
      OrganizingCounts counts = OrganizingCounts.from(latest);
      notificationHelper.notifyCompletion(listId, counts.summary());
      progressStore.update(
          new OrganizingProgressStore.Progress(
              listId, latest.size(), latest.size(), counts.summary(), false, false));
      progressStore.clear(listId);
    } catch (Exception ignored) {
      notificationHelper.cancel(listId);
      progressStore.clear(listId);
    }
  }

  @Override
  public void onDestroy() {
    stopped.set(true);
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
