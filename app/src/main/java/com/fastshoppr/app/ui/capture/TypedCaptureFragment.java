/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentTypedCaptureBinding;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class TypedCaptureFragment extends Fragment {

  private FragmentTypedCaptureBinding binding;
  private TypedCaptureViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentTypedCaptureBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(TypedCaptureViewModel.class);

    binding.toolbar.setTitle(R.string.capture_typed_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());
    binding.btnContinue.setOnClickListener(
        v ->
            viewModel.capture(
                binding.itemInput.getText() == null ? "" : binding.itemInput.getText().toString()));

    viewModel
        .getState()
        .observe(getViewLifecycleOwner(), state -> binding.btnContinue.setEnabled(!state.saving()));
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
  }

  private void handleEvent(SingleEvent<TypedCaptureViewModel.TypedCaptureUiEvent> event) {
    TypedCaptureViewModel.TypedCaptureUiEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof TypedCaptureViewModel.TypedCaptureUiEvent.NavigateToReview navigation) {
      Navigation.findNavController(requireView())
          .navigate(
              R.id.action_capture_typed_to_review, NavArguments.forCapture(navigation.captureId()));
    } else if (content instanceof TypedCaptureViewModel.TypedCaptureUiEvent.ShowError error) {
      Snackbar.make(binding.getRoot(), UiErrorText.stringRes(error.error()), Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
