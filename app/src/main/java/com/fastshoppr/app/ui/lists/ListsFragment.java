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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentListsBinding;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.ZoneId;

@AndroidEntryPoint
public final class ListsFragment extends Fragment implements ShoppingListAdapter.Listener {

  private FragmentListsBinding binding;
  private ListsViewModel viewModel;
  private ShoppingListAdapter adapter;

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
    viewModel = new ViewModelProvider(this).get(ListsViewModel.class);
    adapter = new ShoppingListAdapter(this);
    binding.listsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.listsRecycler.setAdapter(adapter);

    binding.createListButton.setOnClickListener(v -> showCreateListPicker());
    binding.showArchivedButton.setOnClickListener(v -> viewModel.toggleShowArchived());
    binding.openCalendarButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.calendar));
    binding.openSettingsButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.settings));

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
  }

  private void render(ListsUiState state) {
    binding.showArchivedButton.setText(
        state.isShowArchived() ? R.string.hide_archived : R.string.show_archived);
    if (state.getError() != null) {
      binding.listsRecycler.setVisibility(View.GONE);
      binding.listsState.showMessage(getString(UiErrorText.stringRes(state.getError())));
      return;
    }
    if (state.isLoading()) {
      binding.listsRecycler.setVisibility(View.GONE);
      binding.listsState.showLoading();
      return;
    }
    if (state.isEmpty()) {
      binding.listsRecycler.setVisibility(View.GONE);
      binding.listsState.showAction(
          getString(R.string.lists_empty_title),
          getString(R.string.create_first_list),
          v -> showCreateListPicker());
      return;
    }
    binding.listsState.showContent();
    binding.listsRecycler.setVisibility(View.VISIBLE);
    adapter.submitList(state.getRows());
  }

  private void handleEvent(SingleEvent<ListsUiEvent> event) {
    ListsUiEvent content = event.getContentIfNotHandled();
    if (content == null) {
      return;
    }
    if (content instanceof ListsUiEvent.OpenList openList) {
      Navigation.findNavController(requireView())
          .navigate(R.id.list_detail, NavArguments.forList(openList.listId()));
    } else if (content instanceof ListsUiEvent.ShowError showError) {
      Snackbar.make(
              binding.getRoot(),
              getString(UiErrorText.stringRes(showError.error())),
              Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof ListsUiEvent.ShowMessage showMessage) {
      Snackbar.make(
              binding.getRoot(),
              getString(UiMessageText.stringRes(showMessage.message())),
              Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  private void showCreateListPicker() {
    LocalDate initialDate = LocalDate.now(ZoneId.systemDefault());
    DatePickerUtil.showDatePicker(
        this,
        initialDate,
        date -> viewModel.createList(date, getString(R.string.list_name_default)));
  }

  @Override
  public void onOpenList(String listId) {
    Navigation.findNavController(requireView())
        .navigate(R.id.list_detail, NavArguments.forList(listId));
  }

  @Override
  public void onArchiveList(String listId) {
    viewModel.archiveList(listId);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    adapter = null;
  }
}
