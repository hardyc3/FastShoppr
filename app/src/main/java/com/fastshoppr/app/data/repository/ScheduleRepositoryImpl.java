/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.local.dao.ShoppingScheduleDao;
import com.fastshoppr.app.data.local.entity.ShoppingScheduleEntity;
import com.fastshoppr.app.data.repository.mappers.ShoppingScheduleMapper;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.observation.Observable;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ScheduleRepositoryImpl implements ScheduleRepository {

  private final ShoppingScheduleDao scheduleDao;
  private final Clock clock;

  @Inject
  public ScheduleRepositoryImpl(ShoppingScheduleDao scheduleDao, Clock clock) {
    this.scheduleDao = scheduleDao;
    this.clock = clock;
  }

  @Override
  public Observable<List<ShoppingSchedule>> observeAll() {
    return new LiveDataObservable<>(
        Transformations.map(
            scheduleDao.observeAll(),
            entities -> entities.stream().map(ShoppingScheduleMapper::toDomain).toList()));
  }

  @Override
  public List<ShoppingSchedule> getAll() {
    return scheduleDao.getAll().stream().map(ShoppingScheduleMapper::toDomain).toList();
  }

  @Override
  public ShoppingSchedule getActive() {
    ShoppingScheduleEntity entity = scheduleDao.getActive();
    return entity == null ? null : ShoppingScheduleMapper.toDomain(entity);
  }

  @Override
  public ShoppingSchedule getById(String id) {
    ShoppingScheduleEntity entity = scheduleDao.getById(id);
    if (entity == null) {
      throw new NotFoundException("Schedule not found: " + id);
    }
    return ShoppingScheduleMapper.toDomain(entity);
  }

  @Override
  public void insert(ShoppingSchedule schedule) {
    scheduleDao.insert(ShoppingScheduleMapper.toEntity(schedule));
  }

  @Override
  public void update(ShoppingSchedule schedule) {
    if (scheduleDao.update(ShoppingScheduleMapper.toEntity(schedule)) == 0) {
      throw new NotFoundException("Schedule not found: " + schedule.getId());
    }
  }

  @Override
  public void disableOthers(String exceptId) {
    scheduleDao.disableOthers(exceptId, clock.nowMillis());
  }

  @Override
  public void delete(String id) {
    scheduleDao.deleteById(id);
  }
}
