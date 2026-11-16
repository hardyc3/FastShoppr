/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.date.DatePhraseParser;
import com.fastshoppr.app.domain.date.DatePhraseParser.ParsedDate;
import com.fastshoppr.app.domain.date.NearestListResolver;
import com.fastshoppr.app.domain.date.NearestListResolver.Resolution;
import com.fastshoppr.app.domain.model.ShoppingSchedule;
import com.fastshoppr.app.domain.repository.ScheduleRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class ResolveDatePhraseUseCase {

  public record Result(
      ParsedDate parsed,
      Optional<LocalDate> resolvedDate,
      Optional<Resolution> nearestList,
      boolean requiresSchedule) {}

  private final ScheduleRepository scheduleRepository;
  private final NearestListResolver nearestListResolver;
  private final ExecutorService executor;

  @Inject
  public ResolveDatePhraseUseCase(
      ScheduleRepository scheduleRepository,
      NearestListResolver nearestListResolver,
      ExecutorService executor) {
    this.scheduleRepository = scheduleRepository;
    this.nearestListResolver = nearestListResolver;
    this.executor = executor;
  }

  public CompletableFuture<Optional<Result>> resolve(
      String text, LocalDate currentListDate, LocalDate today) {
    Optional<ParsedDate> parsedOpt = DatePhraseParser.parse(text, currentListDate, today);
    if (parsedOpt.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    ParsedDate parsed = parsedOpt.get();
    return CompletableFuture.supplyAsync(
        () -> {
          LocalDate resolved = parsed.resolvedDate().orElse(null);
          if (parsed.requiresSchedule() && resolved == null) {
            resolved = resolveNextShoppingDay(currentListDate);
          }
          if (resolved == null) {
            return Optional.of(
                new Result(parsed, Optional.empty(), Optional.empty(), parsed.requiresSchedule()));
          }
          boolean past = resolved.isBefore(today);
          ParsedDate adjusted =
              new ParsedDate(
                  parsed.phrase(),
                  parsed.itemText(),
                  Optional.of(resolved),
                  parsed.anchor(),
                  parsed.needsConfirmation() || past,
                  parsed.requiresSchedule(),
                  past
                      ? appendNote(parsed.note(), "Resolved date is before today.")
                      : parsed.note());
          Resolution resolution = nearestListResolver.resolve(resolved, today);
          return Optional.of(
              new Result(adjusted, Optional.of(resolved), Optional.of(resolution), false));
        },
        executor);
  }

  private LocalDate resolveNextShoppingDay(LocalDate currentListDate) {
    ShoppingSchedule schedule = scheduleRepository.getActive();
    if (schedule == null || !schedule.isEnabled()) {
      return null;
    }
    LocalDate candidate = schedule.getNextShoppingDate();
    // advance candidate forward until after current list date
    int guard = 0;
    while (!candidate.isAfter(currentListDate) && guard++ < 1000) {
      candidate =
          switch (schedule.getIntervalUnit()) {
            case DAYS -> candidate.plusDays(schedule.getIntervalValue());
            case WEEKS -> candidate.plusWeeks(schedule.getIntervalValue());
          };
    }
    return candidate.isAfter(currentListDate) ? candidate : null;
  }

  private static String appendNote(String existing, String extra) {
    return existing == null || existing.isBlank() ? extra : existing + " " + extra;
  }
}
