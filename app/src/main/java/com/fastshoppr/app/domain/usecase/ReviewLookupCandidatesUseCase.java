/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.lookup.LookupCandidatesCodec;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Reads persisted lookup candidates for an item and prepares display-ready labels. The item's own
 * text is never changed: candidates are only an alternative aisle/product selection.
 */
public final class ReviewLookupCandidatesUseCase {

  /** Localized strings supplied by the screen; the use case stays pure Java. */
  public interface Labels {
    String unknownProduct();

    String noAisle();

    String baySuffix(String bay);
  }

  public record LabeledCandidate(String itemId, StoreLookupResult.Found candidate, String label) {}

  public record Result(String itemId, boolean showCandidates, List<LabeledCandidate> candidates) {}

  @Inject
  public ReviewLookupCandidatesUseCase() {}

  public Result decode(ListItem item, Labels labels) {
    if (item == null) {
      return new Result(null, false, List.of());
    }
    List<LabeledCandidate> decoded = new ArrayList<>();
    if (item.getLookupStatus() == LookupStatus.NEEDS_REVIEW) {
      for (StoreLookupResult.Found candidate :
          LookupCandidatesCodec.decode(item.getMatchCandidatesJson())) {
        decoded.add(new LabeledCandidate(item.getId(), candidate, label(candidate, labels)));
      }
    }
    List<LabeledCandidate> candidates = List.copyOf(decoded);
    return new Result(item.getId(), !candidates.isEmpty(), candidates);
  }

  private static String label(StoreLookupResult.Found candidate, Labels labels) {
    String name = candidate.getProductName();
    if (name == null || name.isBlank()) {
      name = labels.unknownProduct();
    }
    String aisle = candidate.getAisle() == null ? labels.noAisle() : candidate.getAisle();
    String bay = candidate.getBay();
    if (bay != null && !bay.isBlank()) {
      aisle = aisle + labels.baySuffix(bay);
    }
    return name + " (" + aisle + ")";
  }
}
