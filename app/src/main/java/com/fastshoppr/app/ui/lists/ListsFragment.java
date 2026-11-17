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
import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.HandleOverdueListUseCase;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@AndroidEntryPoint
public final class ListsFragment extends Fragment implements ShoppingListAdapter.Listener {

  private FragmentListsBinding binding;
  private ListsViewModel viewModel;
  private ShoppingListAdapter adapter;

  @Inject ShoppingListRepository listRepository;
  @Inject ListItemRepository itemRepository;
  @Inject ScheduleRepository scheduleRepository;
  @Inject HandleOverdueListUseCase overdueUseCase;
  @Inject OverduePromptTracker overduePromptTracker;
  @Inject Clock clock;
  @Inject ExecutorService executor;

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
  public void onResume() {
    super.onResume();
    checkOverdue();
  }

  private void checkOverdue() {
    if (listRepository == null || executor == null) return;
    executor.execute(
        () -> {
          try {
            LocalDate today = LocalDateTimes.today(clock);
            List<ShoppingList> past = listRepository.getPastListsWithUncheckedItems(today);
            for (ShoppingList pl : past) {
              if (!overduePromptTracker.shouldPrompt(pl.getId())) continue;
              overduePromptTracker.markPrompted(pl.getId());
              int unchecked = 0;
              try {
                unchecked =
                    (int)
                        itemRepository.getByList(pl.getId()).stream()
                            .filter(i -> !i.isChecked())
                            .count();
              } catch (Exception ignored) {
              }
              ShoppingList next = listRepository.findNearestActiveOnOrAfter(today);
              java.time.LocalDate suggestedCreateDate = today.plusDays(1);
              try {
                com.fastshoppr.app.domain.model.ShoppingSchedule s = scheduleRepository.getActive();
                if (s != null && s.isEnabled() && !s.getNextShoppingDate().isBefore(today)) {
                  suggestedCreateDate = s.getNextShoppingDate();
                }
              } catch (Exception ignored) {
              }
              final int u = unchecked;
              final ShoppingList nextList = next;
              final java.time.LocalDate createDate = suggestedCreateDate;
              if (!isAdded() || getView() == null) return;
              requireActivity()
                  .runOnUiThread(() -> showOverdueDialog(pl, u, nextList, today, createDate));
              break; // one prompt at a time per resume
            }
          } catch (Exception ignored) {
          }
        });
  }

  private void showOverdueDialog(
      ShoppingList past,
      int unchecked,
      ShoppingList next,
      LocalDate today,
      LocalDate suggestedCreateDate) {
    if (!isAdded()) return;
    String targetInfo =
        next != null ? next.getShoppingDate().toString() : suggestedCreateDate.toString();
    String msg =
        getResources()
                .getQuantityString(
                    R.plurals.overdue_message,
                    unchecked,
                    unchecked,
                    past.getShoppingDate().toString())
            + "\n"
            + getString(R.string.overdue_target, targetInfo);
    MaterialAlertDialogBuilder b =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.overdue_title)
            .setMessage(msg)
            .setNeutralButton(
                R.string.overdue_keep,
                (d, w) -> {
                  /* keep */
                })
            .setNegativeButton(
                R.string.overdue_missed,
                (d, w) ->
                    overdueUseCase.apply(
                        HandleOverdueListUseCase.Action.MISSED, past.getId(), null, null));
    if (next != null) {
      b.setPositiveButton(
          getString(R.string.overdue_move) + " (" + next.getShoppingDate() + ")",
          (d, w) ->
              overdueUseCase.apply(
                  HandleOverdueListUseCase.Action.MOVE_UNCHECKED,
                  past.getId(),
                  next.getId(),
                  null));
    } else {
      b.setPositiveButton(
          getString(R.string.overdue_create_next) + " (" + suggestedCreateDate + ")",
          (d, w) ->
              overdueUseCase.apply(
                  HandleOverdueListUseCase.Action.MOVE_UNCHECKED,
                  past.getId(),
                  null,
                  suggestedCreateDate));
    }
    b.show();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    adapter = null;
  }
}
