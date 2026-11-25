/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import android.content.Context;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.StoreBrand;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Resource-backed copy for verified Phase 10 brand outcomes. */
@Singleton
public class StoreLookupMessages {

  private final Context context;

  @Inject
  public StoreLookupMessages(@ApplicationContext Context context) {
    this.context = context;
  }

  public String forBrand(StoreBrand brand) {
    if (brand == null) {
      return context.getString(R.string.lookup_manual_only);
    }
    return switch (brand) {
      case WALMART -> "";
      case HOME_DEPOT -> context.getString(R.string.lookup_home_depot_blocked);
      case LOWES -> context.getString(R.string.lookup_lowes_blocked);
      case TARGET -> context.getString(R.string.lookup_target_blocked);
      case SMITHS -> context.getString(R.string.lookup_smiths_no_aisle);
      case CUSTOM -> context.getString(R.string.lookup_manual_only);
    };
  }
}
