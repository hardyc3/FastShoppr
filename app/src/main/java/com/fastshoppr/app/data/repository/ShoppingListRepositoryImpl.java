/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.repository.mappers.ShoppingListMapper;
import com.fastshoppr.app.domain.error.ConflictException;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ShoppingListRepositoryImpl implements ShoppingListRepository {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final FastShopprDatabase database;
  private final ShoppingListDao listDao;
  private final ListItemDao itemDao;
  private final CaptureSessionDao captureDao;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Inject
  public ShoppingListRepositoryImpl(
      FastShopprDatabase database,
      ShoppingListDao listDao,
      ListItemDao itemDao,
      CaptureSessionDao captureDao,
      Clock clock,
      IdGenerator idGenerator) {
    this.database = database;
    this.listDao = listDao;
    this.itemDao = itemDao;
    this.captureDao = captureDao;
    this.clock = clock;
    this.idGenerator = idGenerator;
  }

  @Override
  public LiveData<List<ShoppingList>> observeActive() {
    return Transformations.map(
        listDao.observeActive(),
        entities -> entities.stream().map(ShoppingListMapper::toDomain).toList());
  }

  @Override
  public LiveData<List<ShoppingListSummary>> observeActiveSummaries() {
    return Transformations.map(
        listDao.observeActiveSummaries(),
        entities -> entities.stream().map(ShoppingListMapper::toSummary).toList());
  }

  @Override
  public LiveData<List<ShoppingList>> observeArchived() {
    return Transformations.map(
        listDao.observeArchived(),
        entities -> entities.stream().map(ShoppingListMapper::toDomain).toList());
  }

  @Override
  public LiveData<List<ShoppingListSummary>> observeArchivedSummaries() {
    return Transformations.map(
        listDao.observeArchivedSummaries(),
        entities -> entities.stream().map(ShoppingListMapper::toSummary).toList());
  }

  @Override
  public LiveData<ShoppingList> observeById(String id) {
    return Transformations.map(
        listDao.observeById(id),
        entity -> entity == null ? null : ShoppingListMapper.toDomain(entity));
  }

  @Override
  public List<ShoppingList> getActive() {
    return listDao.getActive().stream().map(ShoppingListMapper::toDomain).toList();
  }

  @Override
  public ShoppingList getById(String id) {
    ShoppingListEntity entity = listDao.getById(id);
    if (entity == null) {
      throw new NotFoundException("List not found: " + id);
    }
    return ShoppingListMapper.toDomain(entity);
  }

  @Override
  public ShoppingList getByIdIncludingDeleted(String id) {
    ShoppingListEntity entity = listDao.getByIdIncludingDeleted(id);
    if (entity == null) {
      throw new NotFoundException("List not found: " + id);
    }
    return ShoppingListMapper.toDomain(entity);
  }

  @Override
  public ShoppingList getOrCreate(
      LocalDate shoppingDate, String name, String scheduleId, boolean isAutoCreated) {
    return database.runInTransaction(
        () -> {
          String date = shoppingDate.format(DATE_FORMAT);
          ShoppingListEntity existing = listDao.findActiveByDate(date);
          if (existing != null) {
            return ShoppingListMapper.toDomain(existing);
          }
          long now = clock.nowMillis();
          ShoppingList list =
              new ShoppingList(
                  idGenerator.newId(),
                  name,
                  shoppingDate,
                  scheduleId,
                  isAutoCreated,
                  false,
                  null,
                  now,
                  now);
          listDao.insert(ShoppingListMapper.toEntity(list));
          return list;
        });
  }

  @Override
  public void update(ShoppingList list) {
    if (listDao.update(ShoppingListMapper.toEntity(list)) == 0) {
      throw new NotFoundException("List not found: " + list.getId());
    }
  }

  @Override
  public void archive(String id) {
    if (listDao.setArchived(id, true, clock.nowMillis()) == 0) {
      throw new NotFoundException("List not found: " + id);
    }
  }

  @Override
  public void softDelete(String id) {
    database.runInTransaction(
        () -> {
          ShoppingListEntity entity = listDao.getByIdIncludingDeleted(id);
          if (entity == null) {
            throw new NotFoundException("List not found: " + id);
          }
          if (entity.deletedAt != null) {
            return;
          }
          long deletedAt = clock.nowMillis();
          listDao.softDelete(id, deletedAt);
          itemDao.softDeleteByList(id, deletedAt);
          captureDao.softDeleteByList(id, deletedAt);
        });
  }

  @Override
  public void restore(String id) {
    database.runInTransaction(
        () -> {
          ShoppingListEntity entity = listDao.getByIdIncludingDeleted(id);
          if (entity == null) {
            throw new NotFoundException("List not found: " + id);
          }
          if (entity.deletedAt == null) {
            return;
          }
          if (listDao.countActiveByDate(entity.shoppingDate) > 0) {
            throw new ConflictException(
                "An active list already exists for date: " + entity.shoppingDate);
          }
          long now = clock.nowMillis();
          listDao.restore(id, entity.deletedAt, now);
          itemDao.restoreByListDeletedAt(id, entity.deletedAt, now);
          captureDao.restoreByListDeletedAt(id, entity.deletedAt);
        });
  }

  @Override
  public int purgeDeletedBefore(long cutoffMillis) {
    return database.runInTransaction(
        () -> {
          int lists = listDao.purgeDeletedBefore(cutoffMillis);
          int items = itemDao.purgeDeletedBefore(cutoffMillis);
          int captures = captureDao.purgeDeletedBefore(cutoffMillis);
          return lists + items + captures;
        });
  }
}
