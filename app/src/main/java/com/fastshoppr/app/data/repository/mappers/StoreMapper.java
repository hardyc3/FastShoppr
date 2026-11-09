/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.StoreEntity;
import com.fastshoppr.app.domain.model.Store;

public final class StoreMapper {

  private StoreMapper() {}

  public static StoreEntity toEntity(Store store) {
    return new StoreEntity(
        store.getId(),
        store.getBrand(),
        store.getName(),
        store.getAddressLine(),
        store.getCity(),
        store.getState(),
        store.getPostalCode(),
        store.getLatitude(),
        store.getLongitude(),
        store.isCustom(),
        store.isDefault(),
        store.isArchived(),
        store.getHoursJson(),
        store.getCreatedAt(),
        store.getUpdatedAt());
  }

  public static Store toDomain(StoreEntity entity) {
    return new Store(
        entity.id,
        entity.brand,
        entity.name,
        entity.addressLine,
        entity.city,
        entity.state,
        entity.postalCode,
        entity.latitude,
        entity.longitude,
        entity.isCustom,
        entity.isDefault,
        entity.isArchived,
        entity.hoursJson,
        entity.createdAt,
        entity.updatedAt);
  }
}
