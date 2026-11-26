/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import android.view.View;
import android.widget.Spinner;
import androidx.test.core.app.ApplicationProvider;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.common.StoreBrandLabel;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** Shared Espresso helpers for store-brand labels and spinner selections. */
public final class StoreLabelMatchers {

  private StoreLabelMatchers() {}

  public static String label(StoreBrand brand) {
    return ApplicationProvider.getApplicationContext().getString(StoreBrandLabel.labelRes(brand));
  }

  public static Matcher<View> withSpinnerValue(String expected) {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(View view) {
        if (!(view instanceof Spinner spinner) || spinner.getSelectedItem() == null) {
          return false;
        }
        return expected.equals(spinner.getSelectedItem().toString());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("spinner with selection \"").appendText(expected).appendText("\"");
      }
    };
  }
}
