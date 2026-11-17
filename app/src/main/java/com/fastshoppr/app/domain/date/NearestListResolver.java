/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import java.time.LocalDate;
import java.util.Optional;
import javax.inject.Inject;

public final class NearestListResolver {

  public record Resolution(
      LocalDate targetDate,
      Optional<ShoppingList> existingList,
      boolean createList,
      boolean needsConfirmation,
      String note) {}

  private final ShoppingListRepository listRepository;

  @Inject
  public NearestListResolver(ShoppingListRepository listRepository) {
    this.listRepository = listRepository;
  }

  public Resolution resolve(LocalDate targetDate, LocalDate today) {
    boolean past = targetDate.isBefore(today);
    ShoppingList existing = listRepository.findNearestActiveOnOrAfter(targetDate);
    if (existing != null) {
      return new Resolution(
          targetDate,
          Optional.of(existing),
          false,
          past,
          past ? "Target date is in the past; confirm before moving." : "");
    }
    return new Resolution(
        targetDate,
        Optional.empty(),
        true,
        past,
        past
            ? "No future list exists and the target is in the past; confirm before creating."
            : "No list exists on or after the target; create one for the target date.");
  }
}
