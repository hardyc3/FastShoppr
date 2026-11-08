/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.sort;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import java.util.Comparator;
import java.util.Objects;

public final class ListItemComparator implements Comparator<ListItem> {

  private final SortDirection direction;

  public ListItemComparator(SortDirection direction) {
    this.direction = Objects.requireNonNull(direction, "direction must not be null");
  }

  @Override
  public int compare(ListItem left, ListItem right) {
    boolean leftAisleUnresolved = AisleComparator.isUnresolved(left.getAisle());
    boolean rightAisleUnresolved = AisleComparator.isUnresolved(right.getAisle());
    if (leftAisleUnresolved != rightAisleUnresolved) {
      return leftAisleUnresolved ? 1 : -1;
    }

    int comparison = 0;
    if (!leftAisleUnresolved) {
      comparison = AisleComparator.compareResolvedValues(left.getAisle(), right.getAisle());
      if (comparison != 0) {
        return applyDirection(comparison);
      }
    }

    boolean leftBayUnresolved = AisleComparator.isUnresolved(left.getBay());
    boolean rightBayUnresolved = AisleComparator.isUnresolved(right.getBay());
    if (leftBayUnresolved != rightBayUnresolved) {
      return leftBayUnresolved ? 1 : -1;
    }
    if (!leftBayUnresolved) {
      comparison = AisleComparator.compareResolvedValues(left.getBay(), right.getBay());
      if (comparison != 0) {
        return applyDirection(comparison);
      }
    }

    return applyDirection(Integer.compare(left.getSortPosition(), right.getSortPosition()));
  }

  public SortDirection getDirection() {
    return direction;
  }

  private int applyDirection(int comparison) {
    return direction == SortDirection.ASC ? comparison : -comparison;
  }
}
