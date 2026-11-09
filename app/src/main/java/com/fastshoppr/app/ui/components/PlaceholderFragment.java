/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import com.fastshoppr.app.databinding.FragmentPlaceholderBinding;

public abstract class PlaceholderFragment extends Fragment {

  private FragmentPlaceholderBinding binding;

  @StringRes
  protected abstract int getTitleRes();

  @StringRes
  protected abstract int getBodyRes();

  protected void configure(FragmentPlaceholderBinding binding) {}

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    binding.placeholderTitle.setText(getTitleRes());
    binding.placeholderBody.setText(getBodyRes());
    binding.primaryButton.setVisibility(View.GONE);
    configure(binding);
  }

  protected final void setPrimaryAction(
      @StringRes int textRes, @NonNull View.OnClickListener listener) {
    binding.primaryButton.setText(textRes);
    binding.primaryButton.setVisibility(View.VISIBLE);
    binding.primaryButton.setOnClickListener(listener);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
