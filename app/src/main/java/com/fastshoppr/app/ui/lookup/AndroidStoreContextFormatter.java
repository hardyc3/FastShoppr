/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import android.content.Context;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.common.StoreBrandLabel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;

/** Resource-backed formatter; keeps user-facing copy out of the ViewModel. */
public final class AndroidStoreContextFormatter implements StoreContextFormatter {

  private final Context context;

  @Inject
  public AndroidStoreContextFormatter(@ApplicationContext Context context) {
    this.context = context;
  }

  @Override
  public String savedBranchesHeader() {
    return context.getString(R.string.organize_saved_branches);
  }

  @Override
  public String savedBranchLine(StoreBrand brand, String storeLabel) {
    return context.getString(
        R.string.organize_saved_branch,
        context.getString(StoreBrandLabel.labelRes(brand)),
        storeLabel);
  }
}
