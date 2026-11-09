/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import java.util.List;

public interface ScheduleRepository {

  LiveData<List<ShoppingSchedule>> observeAll();

  List<ShoppingSchedule> getAll();

  ShoppingSchedule getActive();

  ShoppingSchedule getById(String id);

  void insert(ShoppingSchedule schedule);

  void update(ShoppingSchedule schedule);

  void disableOthers(String exceptId);

  void delete(String id);
}
