/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.Store;

public sealed interface ListDetailRow
    permits ListDetailRow.StoreHeader,
        ListDetailRow.Item,
        ListDetailRow.AttentionHeader,
        ListDetailRow.AddItem {

  record StoreHeader(Store store, int itemCount) implements ListDetailRow {}

  record Item(ListItem item) implements ListDetailRow {}

  record AttentionHeader(String storeId) implements ListDetailRow {}

  record AddItem(Store store, String draftText, long generation) implements ListDetailRow {}
}
