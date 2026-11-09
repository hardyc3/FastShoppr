/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import androidx.fragment.app.Fragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Consumer;

public final class DatePickerUtil {

  private DatePickerUtil() {}

  public static void showDatePicker(
      Fragment fragment, LocalDate initialDate, Consumer<LocalDate> onSelected) {
    long selection = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    MaterialDatePicker<Long> picker =
        MaterialDatePicker.Builder.datePicker().setSelection(selection).build();
    picker.addOnPositiveButtonClickListener(
        utcMillis ->
            onSelected.accept(
                Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()));
    picker.show(fragment.getChildFragmentManager(), "date-picker");
  }
}
