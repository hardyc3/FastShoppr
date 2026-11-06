/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentListsBinding;
import com.fastshoppr.app.ui.navigation.NavArguments;

public final class ListsFragment extends Fragment {

  private FragmentListsBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentListsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    binding.openFirstListButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.list_detail, NavArguments.forList(NavArguments.PLACEHOLDER_LIST_ID)));
    binding.openCalendarButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.calendar));
    binding.openSettingsButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.settings));
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
