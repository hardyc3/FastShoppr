/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;

/** In-process progress for active organizing runs (UI-only, non-sensitive). */
@Singleton
public final class OrganizingProgressStore {

  public static final class Progress {
    public final String listId;
    public final int done;
    public final int total;
    public final String status;
    public final boolean running;
    public final boolean walmartNeedsForeground;

    public Progress(
        String listId,
        int done,
        int total,
        String status,
        boolean running,
        boolean walmartNeedsForeground) {
      this.listId = listId;
      this.done = done;
      this.total = total;
      this.status = status;
      this.running = running;
      this.walmartNeedsForeground = walmartNeedsForeground;
    }
  }

  private final MutableLiveData<Map<String, Progress>> live = new MutableLiveData<>(Map.of());
  private final Map<String, Progress> map = new LinkedHashMap<>();
  private final AtomicBoolean suppressCompletion = new AtomicBoolean(false);

  @Inject
  public OrganizingProgressStore() {}

  public LiveData<Map<String, Progress>> observe() {
    return live;
  }

  /**
   * Set while "Delete all data" tears down in-flight runs. The lookup service reads this before
   * posting any completion notification so no stale organizing state appears after a reset.
   */
  public void setSuppressCompletion(boolean suppress) {
    suppressCompletion.set(suppress);
  }

  public boolean shouldSuppressCompletion() {
    return suppressCompletion.get();
  }

  @Nullable
  public synchronized Progress get(String listId) {
    return map.get(listId);
  }

  public synchronized void update(Progress progress) {
    if (progress == null || progress.listId == null) {
      return;
    }
    map.put(progress.listId, progress);
    live.postValue(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
  }

  public synchronized void clear(String listId) {
    if (listId == null) {
      return;
    }
    map.remove(listId);
    live.postValue(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
  }

  public synchronized void clearAll() {
    map.clear();
    live.postValue(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
  }
}
