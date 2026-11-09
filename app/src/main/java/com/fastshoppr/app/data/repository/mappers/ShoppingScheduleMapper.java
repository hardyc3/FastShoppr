/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.ShoppingScheduleEntity;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ShoppingScheduleMapper {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private ShoppingScheduleMapper() {}

  public static ShoppingScheduleEntity toEntity(ShoppingSchedule schedule) {
    return new ShoppingScheduleEntity(
        schedule.getId(),
        schedule.getIntervalValue(),
        schedule.getIntervalUnit(),
        schedule.getNextShoppingDate().format(DATE_FORMAT),
        schedule.isEnabled(),
        schedule.isNotifyOnCreate(),
        formatNullableDate(schedule.getLastCreatedDate()),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }

  public static ShoppingSchedule toDomain(ShoppingScheduleEntity entity) {
    return new ShoppingSchedule(
        entity.id,
        entity.intervalValue,
        entity.intervalUnit,
        LocalDate.parse(entity.nextShoppingDate, DATE_FORMAT),
        entity.isEnabled,
        entity.notifyOnCreate,
        parseNullableDate(entity.lastCreatedDate),
        entity.createdAt,
        entity.updatedAt);
  }

  private static String formatNullableDate(LocalDate value) {
    return value == null ? null : value.format(DATE_FORMAT);
  }

  private static LocalDate parseNullableDate(String value) {
    return value == null ? null : LocalDate.parse(value, DATE_FORMAT);
  }
}
