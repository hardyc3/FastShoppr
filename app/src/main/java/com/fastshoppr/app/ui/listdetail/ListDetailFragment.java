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
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.DateText;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import com.fastshoppr.app.ui.common.SingleEvent;
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

  @Inject StoreRepository storeRepository;
  @Inject SettingsRepository settingsRepository;
  @Inject Clock clock;
  @Inject ExecutorService executor;

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

    binding.sortToggleButton.setOnClickListener(v -> viewModel.toggleSortDirection());
    binding.addStoreButton.setOnClickListener(v -> showAddStoreDialog());
    binding.captureWriteButton.setOnClickListener(v -> openCapture(R.id.capture_ink, false));
    binding.capturePhotoButton.setOnClickListener(v -> openCapture(R.id.capture_camera, false));
    binding.organizeButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.lookup,
                    NavArguments.forList(requireArguments().getString(NavArguments.LIST_ID))));
    binding.storeHoursButton.setOnClickListener(v -> showStoreHoursDialog());

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
    binding.sortToggleButton.setContentDescription(
        state.getSortDirection() == SortDirection.ASC
            ? getString(R.string.sort_ascending)
            : getString(R.string.sort_descending));
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
    } else if (content instanceof ListDetailUiEvent.RequestDeferral req) {
      showDeferralDialog(req);
    } else if (content instanceof ListDetailUiEvent.NeedsScheduleDate req) {
      showNeedsScheduleDialog(req);
    }
  }

  private void showDeferralDialog(ListDetailUiEvent.RequestDeferral req) {
    String targetText = req.targetListDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    String msg = getString(R.string.defer_parsed_message, targetText);
    if (!req.resolvedDate().equals(req.targetListDate())) {
      msg = msg + "\n(" + getString(R.string.defer_nearest_list_note, req.resolvedDate().toString(), targetText) + ")";
    }
    if (req.needsConfirmation()) {
      msg = msg + "\n\n" + getString(R.string.defer_confirm_date_warning);
    }
    final boolean[] remove = {true};
    com.google.android.material.dialog.MaterialAlertDialogBuilder b =
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.defer_title)
            .setMessage(msg + (req.note() == null || req.note().isBlank() ? "" : "\n\n" + req.note()))
            .setMultiChoiceItems(new String[] {getString(R.string.defer_remove_phrase)}, new boolean[] {true},
                (d, which, isChecked) -> remove[0] = isChecked)
            .setPositiveButton(R.string.defer_confirm, (d, w) -> viewModel.confirmDeferredAdd(
                req.storeId(), req.originalText(), req.itemTextWithoutPhrase(),
                req.targetListDate(), req.existingTargetListId(), remove[0]))
            .setNegativeButton(R.string.cancel, null);
    b.show();
  }

  private void showNeedsScheduleDialog(ListDetailUiEvent.NeedsScheduleDate req) {
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.defer_needs_schedule_title)
        .setMessage(R.string.defer_needs_schedule)
        .setPositiveButton(R.string.defer_choose_date, (d, w) ->
            DatePickerUtil.showDatePicker(this,
                java.time.LocalDate.now(),
                date -> viewModel.confirmDeferredAdd(req.storeId(), req.originalText(), req.itemTextWithoutPhrase(), date, null, true)))
        .setNeutralButton(R.string.defer_open_schedule, (d, w) ->
            Navigation.findNavController(requireView()).navigate(R.id.schedule))
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showStoreHoursDialog() {
    ListDetailUiState state = viewModel.getState().getValue();
    if (state == null || state.getStores().isEmpty()) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.store_hours_title)
          .setMessage(R.string.store_hours_unknown)
          .setPositiveButton(R.string.ok, null)
          .show();
      return;
    }
    String[] names = new String[state.getStores().size()];
    for (int i = 0; i < state.getStores().size(); i++) names[i] = state.getStores().get(i).getName();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.store_hours_title)
        .setItems(names, (d, which) -> showStoreHoursFor(state.getStores().get(which), state.getShoppingDate()))
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showStoreHoursFor(Store store, java.time.LocalDate date) {
    String hours = store.getHoursJson();
    boolean hasHours = hours != null && !hours.isBlank();
    String base = hasHours
        ? getString(R.string.store_hours_for_date, date.toString(), hours.trim())
        : getString(R.string.store_hours_unknown);
    // Open/closed shown only when hours explicitly present and parseable; not parsed here -> unavailable.
    String openClosed = hasHours
        ? getString(R.string.store_hours_open_closed_unavailable)
        : getString(R.string.store_hours_open_closed_unknown);
    String pref = settingsRepository == null ? null : settingsRepository.getPreferredShoppingTime();
    String suggestion;
    if (!hasHours || pref == null || pref.isBlank()) {
      suggestion = getString(R.string.visit_suggestion_unknown);
    } else {
      suggestion = getString(R.string.visit_suggestion, pref + " (" + getString(R.string.visit_suggestion_verify_hours) + ")");
    }
    String message = base + "\n" + openClosed + "\n\n" + suggestion;
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(store.getName())
        .setMessage(message)
        .setPositiveButton(R.string.ok, null)
        .setNeutralButton(R.string.edit_store_hours, (d, w) -> showEditHours(store))
        .show();
  }

  private void showEditHours(Store store) {
    final android.widget.EditText input = new android.widget.EditText(requireContext());
    input.setHint("e.g. Mon-Sun 8:00 AM - 10:00 PM");
    if (store.getHoursJson() != null) input.setText(store.getHoursJson());
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.edit_store_hours)
        .setView(input)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, (d, w) -> {
          String text = input.getText() == null ? null : input.getText().toString().trim();
          if (executor == null || storeRepository == null) return;
          executor.execute(() -> {
            try {
              Store updated = new Store(store.getId(), store.getBrand(), store.getName(),
                  store.getAddressLine(), store.getCity(), store.getState(), store.getPostalCode(),
                  store.getLatitude(), store.getLongitude(), store.isCustom(), store.isDefault(),
                  store.isArchived(), text == null || text.isEmpty() ? null : text,
                  store.getCreatedAt(), clock.nowMillis());
              storeRepository.save(updated);
              if (isAdded()) requireActivity().runOnUiThread(() ->
                  Snackbar.make(binding.getRoot(), R.string.store_hours_saved, Snackbar.LENGTH_SHORT).show());
            } catch (Exception ignored) { }
          });
        })
        .show();
  }

  private void showAddStoreDialog() {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_store, null);
    android.widget.Spinner spinner = dialogView.findViewById(R.id.store_spinner);
    android.widget.EditText nameInput = dialogView.findViewById(R.id.store_name_input);
    if (nameInput != null) {
      nameInput.setEnabled(true);
    }
    if (spinner != null) {
      spinner.setVisibility(View.GONE);
    }
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_store_title)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_store_add, null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      String name = nameInput == null ? "" : textOf(nameInput.getText());
                      if (!name.trim().isEmpty()) {
                        viewModel.addStore(name.trim());
                        dialog.dismiss();
                      }
                    }));
    if (nameInput != null) {
      nameInput.setOnEditorActionListener(
          (v, actionId, event) -> {
            String name = textOf(nameInput.getText());
            if (!name.trim().isEmpty()) {
              viewModel.addStore(name.trim());
              dialog.dismiss();
              return true;
            }
            return false;
          });
    }
    dialog.show();
  }

  private void showStorePicker() {
    // Store selection is now handled via the organize flow (WalmartStoreFragment).
    // This method is kept for compatibility but does nothing.
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
  public void onItemChecked(String itemId, boolean checked) {}

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
