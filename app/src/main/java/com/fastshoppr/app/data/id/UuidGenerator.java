/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.id;

import com.fastshoppr.app.domain.id.IdGenerator;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class UuidGenerator implements IdGenerator {

  @Inject
  public UuidGenerator() {}

  @Override
  public String newId() {
    return UUID.randomUUID().toString();
  }
}
