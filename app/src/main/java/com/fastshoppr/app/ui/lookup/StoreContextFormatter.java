/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import com.fastshoppr.app.domain.model.StoreBrand;

/** Formats saved-branch context text with localized resources. */
public interface StoreContextFormatter {

  String savedBranchesHeader();

  String savedBranchLine(StoreBrand brand, String storeLabel);
}
