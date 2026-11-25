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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentReviewBinding;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StorePickerDialog;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public final class ReviewFragment extends Fragment implements ReviewAdapter.Listener {

  private FragmentReviewBinding binding;
  private ReviewViewModel viewModel;
  private ReviewAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentReviewBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

    binding.toolbar.setTitle(R.string.capture_review_toolbar_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    adapter = new ReviewAdapter(this);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerView.setAdapter(adapter);

    binding.checkSelectAll.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            viewModel.selectAll();
          } else {
            viewModel.deselectAll();
          }
        });
    binding.btnDeleteSelected.setOnClickListener(v -> viewModel.deleteSelected());
    binding.btnMergeSelected.setOnClickListener(v -> viewModel.mergeSelected());
    binding.btnAddLine.setOnClickListener(v -> viewModel.addLine());
    binding.btnSplit.setOnClickListener(v -> onSplitClicked());
    binding.btnSave.setOnClickListener(v -> viewModel.saveSelected());
    binding.btnDiscard.setOnClickListener(v -> viewModel.discard());
    binding.btnCleanup.setOnClickListener(v -> openCleanupSheet());

    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
    viewModel.getCleanupState().observe(getViewLifecycleOwner(), this::renderCleanupState);
    viewModel.loadCapture();
  }

  private void openCleanupSheet() {
    if (getChildFragmentManager().findFragmentByTag(AiCleanupDialogFragment.TAG) != null) {
      return;
    }
    AiCleanupDialogFragment.newInstance()
        .show(getChildFragmentManager(), AiCleanupDialogFragment.TAG);
  }

  private void renderCleanupState(ReviewViewModel.CleanupState state) {
    if (binding == null) {
      return;
    }
    binding.btnCleanup.setVisibility(state.isAiEnabled() ? View.VISIBLE : View.GONE);
    int count = state.getSuggestions().size();
    binding.btnCleanup.setText(
        count > 0
            ? getResources().getQuantityString(R.plurals.ai_cleanup_action_count, count, count)
            : getString(R.string.ai_cleanup_action));
    binding.btnCleanup.setEnabled(!state.isRunning());
  }

  private void onSplitClicked() {
    List<ReviewViewModel.ReviewLine> lines = viewModel.getLines();
    for (int index = 0; index < lines.size(); index++) {
      if (lines.get(index).isSelected()) {
        viewModel.splitLine(index, cursorForPosition(index));
        return;
      }
    }
  }

  private int cursorForPosition(int position) {
    RecyclerView.ViewHolder holder =
        binding.recyclerView.findViewHolderForAdapterPosition(position);
    if (holder instanceof ReviewAdapter.ViewHolder viewHolder && viewHolder.textField.isFocused()) {
      return viewHolder.textField.getSelectionStart();
    }
    return -1;
  }

  private void handleEvent(SingleEvent<ReviewViewModel.ReviewUiEvent> event) {
    ReviewViewModel.ReviewUiEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof ReviewViewModel.ReviewUiEvent.CaptureLoaded) {
      onCaptureLoaded();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.LoadFailed loadFailed) {
      Snackbar.make(
              binding.getRoot(), UiErrorText.stringRes(loadFailed.error()), Snackbar.LENGTH_SHORT)
          .show();
      Navigation.findNavController(requireView()).popBackStack();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.LinesChanged) {
      refreshLines();
      syncSelectAll();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.SelectionChanged) {
      refreshLines();
      syncSelectAll();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.NavigateToInk navigate) {
      Navigation.findNavController(requireView())
          .navigate(
              R.id.review_to_ink,
              NavArguments.forCaptureInList(navigate.listId(), navigate.captureId()));
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.SaveCompleted
        || content instanceof ReviewViewModel.ReviewUiEvent.Discarded) {
      navigateBackAfterReview();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.ShowError error) {
      Snackbar.make(binding.getRoot(), UiErrorText.stringRes(error.error()), Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.ShowMessage message) {
      Snackbar.make(
              binding.getRoot(), UiMessageText.stringRes(message.message()), Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  private void onCaptureLoaded() {
    if (binding == null) {
      return;
    }
    adapter.submitLines(viewModel.getLines());
    syncSelectAll();
    ItemSource source = viewModel.getSource();
    boolean photoSource = source == ItemSource.CAMERA_OCR || source == ItemSource.GALLERY_OCR;
    binding.reviewNote.setVisibility(photoSource ? View.VISIBLE : View.GONE);
    binding.btnAddLine.setVisibility(viewModel.isAddLineAvailable() ? View.VISIBLE : View.GONE);
  }

  private void syncSelectAll() {
    if (binding == null) {
      return;
    }
    binding.checkSelectAll.setVisibility(viewModel.getLines().isEmpty() ? View.GONE : View.VISIBLE);
    boolean allSelected =
        !viewModel.getLines().isEmpty()
            && viewModel.getSelectedCount() == viewModel.getLines().size();
    if (binding.checkSelectAll.isChecked() != allSelected) {
      binding.checkSelectAll.setOnCheckedChangeListener(null);
      binding.checkSelectAll.setChecked(allSelected);
      binding.checkSelectAll.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            if (isChecked) {
              viewModel.selectAll();
            } else {
              viewModel.deselectAll();
            }
          });
    }
  }

  private void navigateBackAfterReview() {
    androidx.navigation.NavController controller = Navigation.findNavController(requireView());
    if (!controller.popBackStack(R.id.list_detail, false)) {
      controller.popBackStack();
    }
  }

  private void refreshLines() {
    if (adapter != null) {
      adapter.submitLines(viewModel.getLines());
    }
  }

  @Override
  public void onLineSelected(int position, boolean selected) {
    viewModel.setLineSelected(position, selected);
  }

  @Override
  public void onLineTextChanged(int position, String text) {
    viewModel.setLineText(position, text);
  }

  @Override
  public void onStoreClicked(int position) {
    ReviewViewModel.ReviewLine line = viewModel.getLine(position);
    if (line == null) {
      return;
    }
    StorePickerDialog.show(
        requireContext(),
        viewModel.getStores(),
        line.getStoreId(),
        store -> viewModel.setLineStore(position, store.getId()));
  }

  @Override
  public void onDateClicked(int position) {
    ReviewViewModel.ReviewLine line = viewModel.getLine(position);
    if (line == null) {
      return;
    }
    DatePickerUtil.showDatePicker(
        this,
        viewModel.initialDateFor(line.getTargetDate()),
        date -> viewModel.setLineDate(position, date));
  }

  @Override
  public String storeLabel(@Nullable String storeId) {
    for (Store store : viewModel.getStores()) {
      if (store.getId().equals(storeId)) {
        return store.getName();
      }
    }
    return getString(R.string.select_store);
  }

  @Override
  public String dateLabel(@Nullable String value) {
    return viewModel.dateLabel(value, getString(R.string.field_target_date));
  }

  @Override
  public boolean isLowConfidence(ReviewViewModel.ReviewLine line) {
    return viewModel.isLowConfidence(line);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (viewModel != null
        && !NavArguments.PLACEHOLDER_CAPTURE_ID.equals(viewModel.getCaptureId())) {
      viewModel.reloadCapture();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (viewModel != null) {
      viewModel.onScreenStarted();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (viewModel != null) {
      viewModel.onScreenStopped();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    adapter = null;
  }
}
