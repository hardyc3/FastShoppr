/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import android.content.Context;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.Store;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.function.Consumer;

public final class StorePickerDialog {

  private StorePickerDialog() {}

  public static void show(
      Context context, List<Store> stores, String selectedStoreId, Consumer<Store> onSelected) {
    String[] labels = new String[stores.size()];
    int initialSelection = -1;
    for (int index = 0; index < stores.size(); index++) {
      Store store = stores.get(index);
      String address = StoreFormatter.formatAddress(store);
      labels[index] = address == null ? store.getName() : store.getName() + "\n" + address;
      if (store.getId().equals(selectedStoreId)) {
        initialSelection = index;
      }
    }

    int[] selection = {initialSelection};
    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_store)
        .setSingleChoiceItems(labels, initialSelection, (dialog, which) -> selection[0] = which)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              if (selection[0] >= 0 && selection[0] < stores.size()) {
                onSelected.accept(stores.get(selection[0]));
              }
            })
        .show();
  }
}
