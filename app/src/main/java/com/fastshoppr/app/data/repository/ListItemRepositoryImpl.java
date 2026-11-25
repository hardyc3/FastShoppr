/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import androidx.lifecycle.Transformations;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.repository.mappers.ListItemMapper;
import com.fastshoppr.app.data.repository.mappers.ShoppingListMapper;
import com.fastshoppr.app.domain.error.NotFoundException;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.observation.Observable;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ListItemRepositoryImpl implements ListItemRepository {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final FastShopprDatabase database;
  private final ListItemDao itemDao;
  private final ShoppingListDao listDao;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Inject
  public ListItemRepositoryImpl(
      FastShopprDatabase database,
      ListItemDao itemDao,
      ShoppingListDao listDao,
      Clock clock,
      IdGenerator idGenerator) {
    this.database = database;
    this.itemDao = itemDao;
    this.listDao = listDao;
    this.clock = clock;
    this.idGenerator = idGenerator;
  }

  @Override
  public Observable<List<ListItem>> observeByList(String listId) {
    return new LiveDataObservable<>(
        Transformations.map(
            itemDao.observeByList(listId),
            entities -> entities.stream().map(ListItemMapper::toDomain).toList()));
  }

  @Override
  public Observable<List<ListItem>> observeByListAndStore(String listId, String storeId) {
    return new LiveDataObservable<>(
        Transformations.map(
            itemDao.observeByListAndStore(listId, storeId),
            entities -> entities.stream().map(ListItemMapper::toDomain).toList()));
  }

  @Override
  public Observable<ListItem> observeById(String id) {
    return new LiveDataObservable<>(
        Transformations.map(
            itemDao.observeById(id),
            entity -> entity == null ? null : ListItemMapper.toDomain(entity)));
  }

  @Override
  public List<ListItem> getByList(String listId) {
    return itemDao.getByList(listId).stream().map(ListItemMapper::toDomain).toList();
  }

  @Override
  public ListItem getById(String id) {
    ListItemEntity entity = itemDao.getById(id);
    if (entity == null) {
      throw new NotFoundException("Item not found: " + id);
    }
    return ListItemMapper.toDomain(entity);
  }

  @Override
  public int nextSortPosition(String listId, String storeId) {
    Integer max = itemDao.getMaxSortPosition(listId, storeId);
    return max == null ? 0 : max + 1;
  }

  @Override
  public void insert(ListItem item) {
    itemDao.insert(ListItemMapper.toEntity(item));
  }

  @Override
  public void update(ListItem item) {
    if (itemDao.update(ListItemMapper.toEntity(item)) == 0) {
      throw new NotFoundException("Item not found: " + item.getId());
    }
  }

  @Override
  public void setChecked(String id, boolean checked) {
    if (itemDao.setChecked(id, checked, clock.nowMillis()) == 0) {
      throw new NotFoundException("Item not found: " + id);
    }
  }

  @Override
  public void softDelete(String id) {
    if (itemDao.softDelete(id, clock.nowMillis()) == 0) {
      throw new NotFoundException("Item not found: " + id);
    }
  }

  @Override
  public ListItem moveToDate(String itemId, LocalDate targetDate, String targetListName) {
    return database.runInTransaction(
        () -> {
          ListItemEntity item = itemDao.getById(itemId);
          if (item == null) {
            throw new NotFoundException("Item not found: " + itemId);
          }

          String date = targetDate.format(DATE_FORMAT);
          ShoppingListEntity targetList = listDao.findActiveByDate(date);
          if (targetList == null) {
            long now = clock.nowMillis();
            ShoppingList draft =
                new ShoppingList(
                    idGenerator.newId(),
                    targetListName,
                    targetDate,
                    null,
                    false,
                    false,
                    null,
                    now,
                    now);
            targetList = ShoppingListMapper.toEntity(draft);
            listDao.insert(targetList);
          }

          Integer max = itemDao.getMaxSortPosition(targetList.id, item.storeId);
          int sortPosition = max == null ? 0 : max + 1;
          itemDao.moveToDate(itemId, targetList.id, date, sortPosition, clock.nowMillis());
          return ListItemMapper.toDomain(itemDao.getById(itemId));
        });
  }

  @Override
  public int moveUncheckedItemsToList(
      String sourceListId, String targetListId, LocalDate targetDate) {
    return database.runInTransaction(
        () -> {
          List<ListItemEntity> unchecked = itemDao.getUncheckedByList(sourceListId);
          String date = targetDate.format(DATE_FORMAT);
          int moved = 0;
          long now = clock.nowMillis();
          for (ListItemEntity item : unchecked) {
            Integer max = itemDao.getMaxSortPosition(targetListId, item.storeId);
            int sortPosition = max == null ? 0 : max + 1;
            itemDao.moveToDate(item.id, targetListId, date, sortPosition, now);
            moved++;
          }
          return moved;
        });
  }

  @Override
  public int purgeDeletedBefore(long cutoffMillis) {
    return itemDao.purgeDeletedBefore(cutoffMillis);
  }
}
