/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.domain.model.ListItem;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ListItemMapper {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private ListItemMapper() {}

  public static ListItemEntity toEntity(ListItem item) {
    return new ListItemEntity(
        item.getId(),
        item.getListId(),
        item.getStoreId(),
        item.getRawText(),
        item.getDisplayName(),
        item.getNormalizedText(),
        item.getQuantity(),
        item.getTargetDate().format(DATE_FORMAT),
        item.getAisle(),
        item.getBay(),
        item.getMatchedProductName(),
        item.getMatchedProductUrl(),
        item.getMatchConfidence(),
        item.getLookupStatus(),
        item.getLookupSource(),
        item.getLookupError(),
        item.getSource(),
        item.isChecked(),
        item.getSortPosition(),
        item.getDeletedAt(),
        item.getCreatedAt(),
        item.getUpdatedAt());
  }

  public static ListItem toDomain(ListItemEntity entity) {
    return new ListItem(
        entity.id,
        entity.listId,
        entity.storeId,
        entity.rawText,
        entity.displayName,
        entity.normalizedText,
        entity.quantity,
        LocalDate.parse(entity.targetDate, DATE_FORMAT),
        entity.aisle,
        entity.bay,
        entity.matchedProductName,
        entity.matchedProductUrl,
        entity.matchConfidence,
        entity.lookupStatus,
        entity.lookupSource,
        entity.lookupError,
        entity.source,
        entity.isChecked,
        entity.sortPosition,
        entity.deletedAt,
        entity.createdAt,
        entity.updatedAt);
  }
}
