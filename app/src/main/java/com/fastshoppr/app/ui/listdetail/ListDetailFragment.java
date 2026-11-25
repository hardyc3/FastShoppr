/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.DialogSetAisleBinding;
import com.fastshoppr.app.databinding.FragmentListDetailBinding;
import com.fastshoppr.app.domain.lookup.LookupCandidatesCodec;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.DateText;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public final class ListDetailFragment extends Fragment implements ListDetailAdapter.Listener {

  private FragmentListDetailBinding binding;
  private ListDetailViewModel viewModel;
  private ListDetailAdapter adapter;
  private List<Store> addableStores = List.of();

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
    binding.addStoreButton.setOnClickListener(v -> showAddStoreDialog(addableStores));
    binding.captureWriteButton.setOnClickListener(v -> openCapture(R.id.capture_ink, false));
    binding.capturePhotoButton.setOnClickListener(v -> openCapture(R.id.capture_camera, false));
    binding.organizeButton.setOnClickListener(
        v ->
            Navigation.findNavController(v)
                .navigate(
                    R.id.lookup,
                    NavArguments.forList(requireArguments().getString(NavArguments.LIST_ID))));

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
    viewModel
        .getAddableStores()
        .observe(
            getViewLifecycleOwner(), stores -> addableStores = stores == null ? List.of() : stores);
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
    binding.listDetailName.setText(state.getListName());
    binding.listDetailDate.setText(formatDate(state.getShoppingDate()));
    if (state.getRows().isEmpty()) {
      binding.listDetailRecycler.setVisibility(View.GONE);
      binding.listDetailState.showMessage(getString(R.string.no_stores));
      return;
    }

    binding.listDetailState.showContent();
    binding.listDetailRecycler.setVisibility(View.VISIBLE);
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
    } else if (content instanceof ListDetailUiEvent.NavigateToBranchPicker req) {
      Navigation.findNavController(requireView())
          .navigate(R.id.list_detail_to_branch_picker, NavArguments.forBranch(req.brand()));
    }
  }

  private void showDeferralDialog(ListDetailUiEvent.RequestDeferral req) {
    String targetText = req.targetListDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    String msg = getString(R.string.defer_parsed_message, targetText);
    if (!req.resolvedDate().equals(req.targetListDate())) {
      msg =
          msg
              + "\n("
              + getString(
                  R.string.defer_nearest_list_note, req.resolvedDate().toString(), targetText)
              + ")";
    }
    if (req.needsConfirmation()) {
      msg = msg + "\n\n" + getString(R.string.defer_confirm_date_warning);
    }
    final boolean[] remove = {true};
    MaterialAlertDialogBuilder b =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.defer_title)
            .setMessage(
                msg + (req.note() == null || req.note().isBlank() ? "" : "\n\n" + req.note()))
            .setMultiChoiceItems(
                new String[] {getString(R.string.defer_remove_phrase)},
                new boolean[] {true},
                (d, which, isChecked) -> remove[0] = isChecked)
            .setPositiveButton(
                R.string.defer_confirm,
                (d, w) ->
                    viewModel.confirmDeferredAdd(
                        req.storeId(),
                        req.originalText(),
                        req.itemTextWithoutPhrase(),
                        req.targetListDate(),
                        req.existingTargetListId(),
                        remove[0]))
            .setNegativeButton(R.string.cancel, null);
    b.show();
  }

  private void showNeedsScheduleDialog(ListDetailUiEvent.NeedsScheduleDate req) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.defer_needs_schedule_title)
        .setMessage(R.string.defer_needs_schedule)
        .setPositiveButton(
            R.string.defer_choose_date,
            (d, w) ->
                DatePickerUtil.showDatePicker(
                    this,
                    LocalDate.now(),
                    date ->
                        viewModel.confirmDeferredAdd(
                            req.storeId(),
                            req.originalText(),
                            req.itemTextWithoutPhrase(),
                            date,
                            null,
                            true)))
        .setNeutralButton(
            R.string.defer_open_schedule,
            (d, w) -> Navigation.findNavController(requireView()).navigate(R.id.schedule))
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showAddStoreDialog(List<Store> stores) {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_store, null);
    Spinner spinner = dialogView.findViewById(R.id.store_spinner);
    EditText nameInput = dialogView.findViewById(R.id.store_name_input);

    List<String> labels = new ArrayList<>();
    labels.add(getString(R.string.add_store_select));
    for (Store store : stores) {
      labels.add(store.getName());
    }
    ArrayAdapter<String> spinnerAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(spinnerAdapter);
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position > 0) {
              nameInput.setText("");
            }
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    nameInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (s.length() > 0 && spinner.getSelectedItemPosition() != 0) {
              spinner.setSelection(0, false);
            }
          }
        });

    AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_store_title)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_store_add, null)
            .create();
    Runnable addStore =
        () -> {
          String name = textOf(nameInput.getText()).trim();
          if (!name.isEmpty()) {
            viewModel.addStore(name);
            dialog.dismiss();
            return;
          }
          int position = spinner.getSelectedItemPosition();
          if (position > 0 && position <= stores.size()) {
            viewModel.addStore(stores.get(position - 1));
            dialog.dismiss();
            return;
          }
          nameInput.setError(getString(R.string.add_store_required));
          nameInput.requestFocus();
        };
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> addStore.run()));
    nameInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          addStore.run();
          return true;
        });
    dialog.show();
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
    List<StoreLookupResult.Found> candidates =
        LookupCandidatesCodec.decode(item.getMatchCandidatesJson());
    if (item.getLookupStatus() == LookupStatus.NEEDS_REVIEW && !candidates.isEmpty()) {
      showCandidatePicker(item, candidates);
      return;
    }
    showSetAisleDialog(item);
  }

  private void showCandidatePicker(ListItem item, List<StoreLookupResult.Found> candidates) {
    List<String> labels = new ArrayList<>();
    for (StoreLookupResult.Found c : candidates) {
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
