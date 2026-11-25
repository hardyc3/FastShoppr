/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.domain.observation.Registration;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.CreateListUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@HiltViewModel
public final class CalendarViewModel extends ViewModel {

  private final ShoppingListRepository listRepository;
  private final SettingsRepository settingsRepository;
  private final CreateListUseCase createListUseCase;
  private final Clock clock;

  private final MutableLiveData<CalendarUiState> state = new MutableLiveData<>();
  private Registration monthRegistration;
  private Registration upcomingRegistration;

  private YearMonth month;
  private List<ShoppingListSummary> monthSummaries = List.of();
  private List<ShoppingListSummary> upcomingSummaries = List.of();

  @Inject
  public CalendarViewModel(
      ShoppingListRepository listRepository,
      SettingsRepository settingsRepository,
      CreateListUseCase createListUseCase,
      Clock clock) {
    this.listRepository = listRepository;
    this.settingsRepository = settingsRepository;
    this.createListUseCase = createListUseCase;
    this.clock = clock;
  }

  public LiveData<CalendarUiState> getState() {
    return state;
  }

  public void init() {
    if (month != null) return;
    YearMonth now = YearMonth.from(today());
    String saved = settingsRepository.getLastCalendarMonth();
    try {
      if (saved != null) now = YearMonth.parse(saved);
    } catch (Exception ignored) {
    }
    setMonth(now);
    observeUpcoming();
  }

  public void prevMonth() {
    setMonth(month.minusMonths(1));
  }

  public void nextMonth() {
    setMonth(month.plusMonths(1));
  }

  private void setMonth(YearMonth ym) {
    this.month = ym;
    settingsRepository.setLastCalendarMonth(ym.toString());
    if (monthRegistration != null) {
      monthRegistration.close();
    }
    LocalDate start = ym.atDay(1);
    LocalDate end = ym.atEndOfMonth();
    monthRegistration =
        listRepository
            .observeActiveSummariesInRange(start, end)
            .subscribe(
                summaries -> {
                  monthSummaries = summaries == null ? List.of() : summaries;
                  rebuild();
                });
    rebuild();
  }

  private void observeUpcoming() {
    if (upcomingRegistration != null) return;
    LocalDate start = today();
    LocalDate end = start.plusYears(2);
    upcomingRegistration =
        listRepository
            .observeActiveSummariesInRange(start, end)
            .subscribe(
                summaries -> {
                  upcomingSummaries = summaries == null ? List.of() : summaries;
                  rebuild();
                });
  }

  private void rebuild() {
    if (month == null) return;
    Map<LocalDate, ShoppingListSummary> byDate = new HashMap<>();
    for (ShoppingListSummary s : monthSummaries) {
      byDate.put(s.getList().getShoppingDate(), s);
    }
    LocalDate first = month.atDay(1);
    LocalDate gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    LocalDate gridEnd = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    List<CalendarDay> days = new ArrayList<>();
    LocalDate today = today();
    for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
      boolean inMonth = YearMonth.from(d).equals(month);
      days.add(
          new CalendarDay(
              inMonth ? d : null, inMonth, d.equals(today), inMonth ? byDate.get(d) : null));
    }
    state.setValue(new CalendarUiState(month, days, upcomingSummaries));
  }

  public void createListFor(LocalDate date, String name) {
    createListUseCase.create(date, name, null, false);
  }

  private LocalDate today() {
    return LocalDateTimes.today(clock);
  }

  @Override
  protected void onCleared() {
    if (monthRegistration != null) {
      monthRegistration.close();
      monthRegistration = null;
    }
    if (upcomingRegistration != null) {
      upcomingRegistration.close();
      upcomingRegistration = null;
    }
  }
}
