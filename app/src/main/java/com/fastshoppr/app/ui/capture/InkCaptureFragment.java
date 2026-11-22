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
import com.fastshoppr.app.databinding.FragmentInkCaptureBinding;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class InkCaptureFragment extends Fragment {

  private FragmentInkCaptureBinding binding;
  private InkCaptureViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentInkCaptureBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(InkCaptureViewModel.class);

    binding.toolbar.setTitle(R.string.capture_ink_toolbar_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    binding.btnUndo.setOnClickListener(v -> binding.inkCanvas.undoLastStroke());
    binding.btnClear.setOnClickListener(v -> binding.inkCanvas.resetStrokes());
    binding.btnSave.setOnClickListener(v -> viewModel.save(binding.inkCanvas.getInkData()));

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
  }

  private void render(InkCaptureViewModel.InkCaptureUiState state) {
    if (binding == null) {
      return;
    }
    boolean busy =
        state.recognizing() || state.modelState() == InkCaptureViewModel.ModelState.DOWNLOADING;
    binding.btnSave.setEnabled(!busy);
    binding.btnUndo.setEnabled(!state.recognizing());
    binding.btnClear.setEnabled(!state.recognizing());

    switch (state.modelState()) {
      case CHECKING -> showStatus(true, R.string.ink_model_checking);
      case DOWNLOADING -> showStatus(true, R.string.ink_model_downloading);
      case READY -> {
        if (state.recognizing()) {
          showStatus(true, R.string.ink_recognizing);
        } else {
          binding.inkStatusGroup.setVisibility(View.GONE);
        }
      }
      case FAILED -> {
        showStatus(true, R.string.ink_model_not_ready);
        binding.inkProgress.setVisibility(View.GONE);
      }
    }
  }

  private void showStatus(boolean visible, int textRes) {
    binding.inkStatusGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.inkProgress.setVisibility(View.VISIBLE);
    binding.inkProgress.setIndeterminate(true);
    binding.inkStatus.setText(textRes);
  }

  private void handleEvent(SingleEvent<InkCaptureViewModel.InkUiEvent> event) {
    InkCaptureViewModel.InkUiEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof InkCaptureViewModel.InkUiEvent.NavigateToReview navigate) {
      androidx.navigation.NavController controller = Navigation.findNavController(requireView());
      String existingCaptureId = requireArguments().getString(NavArguments.CAPTURE_ID);
      if (existingCaptureId != null
          && !existingCaptureId.isBlank()
          && controller.popBackStack(R.id.review, false)) {
        return;
      }
      controller.navigate(R.id.review, NavArguments.forCapture(navigate.captureId()));
    } else if (content instanceof InkCaptureViewModel.InkUiEvent.ShowError error) {
      Snackbar.make(binding.getRoot(), UiErrorText.stringRes(error.error()), Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof InkCaptureViewModel.InkUiEvent.ShowMessage message) {
      Snackbar.make(
              binding.getRoot(), UiMessageText.stringRes(message.message()), Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof InkCaptureViewModel.InkUiEvent.StrokesRequired) {
      Snackbar.make(binding.getRoot(), R.string.ink_strokes_required, Snackbar.LENGTH_SHORT).show();
    } else if (content instanceof InkCaptureViewModel.InkUiEvent.EmptyRecognition) {
      showEmptyRecognitionOptions();
    }
  }

  private void showEmptyRecognitionOptions() {
    String listId = requireArguments().getString(NavArguments.LIST_ID);
    EmptyRecognitionDialog.show(
        this,
        () -> {},
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.capture_camera, NavArguments.forListLaunchGallery(listId)),
        () ->
            Navigation.findNavController(requireView())
                .navigate(R.id.capture_typed, NavArguments.forList(listId)));
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
