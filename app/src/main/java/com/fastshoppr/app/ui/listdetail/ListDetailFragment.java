/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentListDetailBinding;
import com.fastshoppr.app.ui.navigation.NavArguments;

public final class ListDetailFragment extends Fragment {

  private FragmentListDetailBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentListDetailBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    String listId = getListId();
    binding.listDetailBody.setText(getString(R.string.list_detail_body, listId));

    binding.openTypedCaptureButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(R.id.capture_typed, NavArguments.forList(listId)));
    binding.openInkCaptureButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(R.id.capture_ink, NavArguments.forList(listId)));
    binding.openCameraCaptureButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(R.id.capture_camera, NavArguments.forList(listId)));
    binding.openLookupButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.lookup, NavArguments.forList(listId)));
  }

  private String getListId() {
    Bundle arguments = getArguments();
    if (arguments == null) {
      return NavArguments.PLACEHOLDER_LIST_ID;
    }
    String listId = arguments.getString(NavArguments.LIST_ID);
    return listId == null ? NavArguments.PLACEHOLDER_LIST_ID : listId;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
