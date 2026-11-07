/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.domain.id.IdGenerator;

public final class FixedIdGenerator implements IdGenerator {

  private int next = 1;

  @Override
  public String newId() {
    return "id-" + next++;
  }
}
