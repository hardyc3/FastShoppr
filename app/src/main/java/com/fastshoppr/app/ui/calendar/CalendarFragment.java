/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentCalendarBinding;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@AndroidEntryPoint
public final class CalendarFragment extends Fragment
    implements CalendarDayAdapter.Listener, CalendarUpcomingAdapter.Listener {

  private FragmentCalendarBinding binding;
  private CalendarViewModel viewModel;
  private CalendarDayAdapter dayAdapter;
  private CalendarUpcomingAdapter upcomingAdapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentCalendarBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
    binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    dayAdapter = new CalendarDayAdapter(this);
    upcomingAdapter = new CalendarUpcomingAdapter(this);
    binding.recyclerDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
    binding.recyclerDays.setAdapter(dayAdapter);
    binding.recyclerUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerUpcoming.setAdapter(upcomingAdapter);
    binding.btnPrevMonth.setOnClickListener(v -> viewModel.prevMonth());
    binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.init();
  }

  private void render(CalendarUiState state) {
    if (binding == null || state == null) return;
    String monthName = state.month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
        + " " + state.month.getYear();
    binding.textMonth.setText(monthName);
    dayAdapter.submit(state.days);
    upcomingAdapter.submit(state.upcoming);
    binding.textNoUpcoming.setVisibility(state.upcoming.isEmpty() ? View.VISIBLE : View.GONE);
    binding.recyclerUpcoming.setVisibility(state.upcoming.isEmpty() ? View.GONE : View.VISIBLE);
  }

  @Override
  public void onDayClick(CalendarDay day) {
    if (day == null || day.date == null) return;
    if (day.summary != null) {
      openList(day.summary.getList().getId());
    } else {
      new MaterialAlertDialogBuilder(requireContext())
          .setMessage(getString(R.string.calendar_create_list_prompt,
              day.date.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          .setPositiveButton(R.string.create_list, (d, w) -> {
            viewModel.createListFor(day.date, getString(R.string.list_name_default));
          })
          .setNegativeButton(R.string.cancel, null)
          .show();
    }
  }

  @Override
  public void onOpen(ShoppingListSummary summary) {
    if (summary != null) openList(summary.getList().getId());
  }

  private void openList(String listId) {
    Navigation.findNavController(requireView())
        .navigate(R.id.list_detail, NavArguments.forList(listId));
  }

  @Override
  public void onDestroyView() { super.onDestroyView(); binding = null; }
}
