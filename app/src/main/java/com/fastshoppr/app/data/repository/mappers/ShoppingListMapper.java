/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.domain.model.ShoppingList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ShoppingListMapper {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private ShoppingListMapper() {}

  public static ShoppingListEntity toEntity(ShoppingList list) {
    return new ShoppingListEntity(
        list.getId(),
        list.getName(),
        list.getShoppingDate().format(DATE_FORMAT),
        list.getScheduleId(),
        list.isAutoCreated(),
        list.isArchived(),
        list.getDeletedAt(),
        list.getCreatedAt(),
        list.getUpdatedAt());
  }

  public static ShoppingList toDomain(ShoppingListEntity entity) {
    return new ShoppingList(
        entity.id,
        entity.name,
        LocalDate.parse(entity.shoppingDate, DATE_FORMAT),
        entity.scheduleId,
        entity.isAutoCreated,
        entity.isArchived,
        entity.deletedAt,
        entity.createdAt,
        entity.updatedAt);
  }
}
