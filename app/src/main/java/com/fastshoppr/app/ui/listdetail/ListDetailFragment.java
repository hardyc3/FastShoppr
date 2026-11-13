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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.DialogSetAisleBinding;
import com.fastshoppr.app.databinding.FragmentListDetailBinding;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.DateText;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StorePickerDialog;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;

@AndroidEntryPoint
public final class ListDetailFragment extends Fragment implements ListDetailAdapter.Listener {

  private FragmentListDetailBinding binding;
  private ListDetailViewModel viewModel;
  private ListDetailAdapter adapter;

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
    viewModel = new ViewModelProvider(this).get(ListDetailViewModel.class);
    adapter = new ListDetailAdapter(this);
    binding.listDetailRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.listDetailRecycler.setAdapter(adapter);

    binding.storeSelectorButton.setOnClickListener(v -> showStorePicker());
    binding.sortToggleButton.setOnClickListener(v -> viewModel.toggleSortDirection());
    binding.captureWriteButton.setOnClickListener(v -> openCapture(R.id.capture_ink, false));
    binding.capturePhotoButton.setOnClickListener(v -> openCapture(R.id.capture_camera, false));
    binding.captureGalleryButton.setOnClickListener(v -> openCapture(R.id.capture_camera, true));
    binding.organizeButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.lookup,
                    NavArguments.forList(requireArguments().getString(NavArguments.LIST_ID))));

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
    viewModel.refresh();
  }

  private void openCapture(int destinationId, boolean launchGallery) {
    String listId = requireArguments().getString(NavArguments.LIST_ID);
    Bundle args =
        launchGallery ? NavArguments.forListLaunchGallery(listId) : NavArguments.forList(listId);
    Navigation.findNavController(requireView()).navigate(destinationId, args);
  }

  private void render(ListDetailUiState state) {
    if (state.getError() != null) {
      binding.listDetailRecycler.setVisibility(View.GONE);
      binding.listDetailState.showAction(
          getString(UiErrorText.stringRes(state.getError())),
          getString(R.string.back_to_lists),
          v -> Navigation.findNavController(requireView()).popBackStack());
      return;
    }
    if (state.isLoading()) {
      binding.listDetailRecycler.setVisibility(View.GONE);
      binding.listDetailState.showLoading();
      return;
    }
    if (state.getRows().isEmpty()) {
      binding.listDetailRecycler.setVisibility(View.GONE);
      binding.listDetailState.showMessage(getString(R.string.no_stores));
      return;
    }

    binding.listDetailState.showContent();
    binding.listDetailRecycler.setVisibility(View.VISIBLE);
    binding.listDetailName.setText(state.getListName());
    binding.listDetailDate.setText(formatDate(state.getShoppingDate()));
    binding.storeSelectorButton.setText(
        state.getSelectedStoreName() == null
            ? getString(R.string.select_store)
            : getString(R.string.store_selector, state.getSelectedStoreName()));
    binding.sortToggleButton.setText(
        state.getSortDirection() == SortDirection.ASC
            ? R.string.sort_ascending
            : R.string.sort_descending);
    adapter.submitList(state.getRows());
  }

  private void handleEvent(SingleEvent<ListDetailUiEvent> event) {
    ListDetailUiEvent content = event.getContentIfNotHandled();
    if (content == null) {
      return;
    }
    if (content instanceof ListDetailUiEvent.ShowError showError) {
      Snackbar.make(
              binding.getRoot(),
              getString(UiErrorText.stringRes(showError.error())),
              Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof ListDetailUiEvent.ShowMessage showMessage) {
      Snackbar.make(
              binding.getRoot(),
              getString(UiMessageText.stringRes(showMessage.message())),
              Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  private void showStorePicker() {
    ListDetailUiState state = viewModel.getState().getValue();
    if (state == null || state.getStores().isEmpty()) {
      return;
    }
    StorePickerDialog.show(
        requireContext(),
        state.getStores(),
        state.getSelectedStoreId(),
        store -> {
          viewModel.selectStore(store.getId());
          scrollToStore(store.getId());
        });
  }

  private void scrollToStore(String storeId) {
    int position = adapter.positionOfStore(storeId);
    RecyclerView.LayoutManager layoutManager = binding.listDetailRecycler.getLayoutManager();
    if (position != RecyclerView.NO_POSITION
        && layoutManager instanceof LinearLayoutManager linear) {
      linear.scrollToPositionWithOffset(position, 0);
    }
  }

  @Override
  public void onItemChecked(String itemId, boolean checked) {
    viewModel.setChecked(itemId, checked);
  }

  @Override
  public void onItemEdit(ListItem item) {
    Navigation.findNavController(requireView())
        .navigate(R.id.item_edit, NavArguments.forItem(item.getId()));
  }

  @Override
  public void onItemDefer(ListItem item) {
    ListDetailUiState state = viewModel.getState().getValue();
    String listName =
        state == null || state.getListName() == null
            ? getString(R.string.list_name_default)
            : state.getListName();
    DatePickerUtil.showDatePicker(
        this, item.getTargetDate(), date -> viewModel.deferItem(item.getId(), date, listName));
  }

  @Override
  public void onItemSetAisle(ListItem item) {
    showSetAisleDialog(item);
  }

  @Override
  public void onItemNotRight(ListItem item) {
    java.util.List<com.fastshoppr.app.domain.lookup.StoreLookupResult.Found> candidates =
        com.fastshoppr.app.domain.lookup.LookupCandidatesCodec.decode(
            item.getMatchCandidatesJson());
    if (item.getLookupStatus() == com.fastshoppr.app.domain.model.LookupStatus.NEEDS_REVIEW
        && !candidates.isEmpty()) {
      showCandidatePicker(item, candidates);
      return;
    }
    showSetAisleDialog(item);
  }

  private void showCandidatePicker(
      ListItem item,
      java.util.List<com.fastshoppr.app.domain.lookup.StoreLookupResult.Found> candidates) {
    java.util.List<String> labels = new java.util.ArrayList<>();
    for (com.fastshoppr.app.domain.lookup.StoreLookupResult.Found c : candidates) {
      String name = c.getProductName();
      if (name == null || name.isBlank()) {
        name = getString(R.string.lookup_candidate_unknown);
      }
      String loc = c.getAisle() == null ? getString(R.string.no_aisle) : c.getAisle();
      if (c.getBay() != null && !c.getBay().isBlank()) {
        loc = loc + getString(R.string.bay_suffix, c.getBay());
      }
      labels.add(name + " (" + loc + ")");
    }
    labels.add(getString(R.string.set_aisle));
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.lookup_candidates_title)
        .setItems(
            labels.toArray(new String[0]),
            (dialog, which) -> {
              if (which >= 0 && which < candidates.size()) {
                viewModel.applyLookupCandidate(item.getId(), candidates.get(which));
              } else {
                showSetAisleDialog(item);
              }
            })
        .show();
  }

  @Override
  public void onItemRefreshLookup(ListItem item) {
    viewModel.refreshLookup(item.getId());
    Snackbar.make(binding.getRoot(), R.string.lookup_run_started, Snackbar.LENGTH_SHORT).show();
  }

  private void showSetAisleDialog(ListItem item) {
    DialogSetAisleBinding dialogBinding = DialogSetAisleBinding.inflate(getLayoutInflater());
    dialogBinding.dialogAisleInput.setText(item.getAisle());
    dialogBinding.dialogBayInput.setText(item.getBay());
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.set_aisle_title)
        .setView(dialogBinding.getRoot())
        .setNegativeButton(android.R.string.cancel, null)
        .setNeutralButton(R.string.refresh_lookup, (d, w) -> viewModel.refreshLookup(item.getId()))
        .setPositiveButton(
            R.string.save,
            (dialog, which) ->
                viewModel.setAisle(
                    item.getId(),
                    textOf(dialogBinding.dialogAisleInput.getText()),
                    textOf(dialogBinding.dialogBayInput.getText())))
        .show();
  }

  @Override
  public void onItemDelete(ListItem item) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_item_confirm)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            R.string.delete_item, (dialog, which) -> viewModel.deleteItem(item.getId()))
        .show();
  }

  @Override
  public void onAddItem(String storeId, String text) {
    viewModel.addItem(storeId, text);
  }

  @Override
  public void onDraftChanged(String storeId, String text) {
    viewModel.saveDraft(storeId, text);
  }

  private static String textOf(CharSequence text) {
    return text == null ? "" : text.toString();
  }

  private static String formatDate(LocalDate date) {
    return DateText.formatMedium(date);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    adapter = null;
  }
}
