/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.sort;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.SortDirection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class AisleComparatorTest {

  @Test
  public void sortsAscendingWithNumericAwareness() {
    List<String> values =
        new ArrayList<>(Arrays.asList("10", "03", "unknown", "A100", "EC2", "2", "A12"));
    values.sort(new AisleComparator(SortDirection.ASC));

    assertEquals(List.of("2", "EC2", "03", "10", "A12", "A100", "unknown"), values);
  }

  @Test
  public void sortsDescendingButKeepsUnresolvedLast() {
    List<String> values = new ArrayList<>(Arrays.asList("2", "10", "unknown", "EC2", "A12", "03"));
    values.sort(new AisleComparator(SortDirection.DESC));

    assertEquals(List.of("A12", "10", "03", "EC2", "2", "unknown"), values);
  }

  @Test
  public void treatsMissingBlankAndNonNumericValuesAsUnresolved() {
    List<String> values = new ArrayList<>(Arrays.asList("A5", null, " ", "Front", "2"));
    values.sort(new AisleComparator(SortDirection.ASC));

    assertEquals("2", values.get(0));
    assertEquals("A5", values.get(1));
    assertEquals(Arrays.asList(null, " ", "Front"), values.subList(2, 5));
  }

  @Test
  public void parsesNumericValueInsideDescriptiveAisle() {
    List<String> values = new ArrayList<>(List.of("Aisle 5", "Aisle 10"));
    values.sort(new AisleComparator(SortDirection.ASC));

    assertEquals(List.of("Aisle 5", "Aisle 10"), values);
  }

  @Test
  public void prefixesAndSuffixesBreakNumericTies() {
    List<String> values = new ArrayList<>(List.of("A12B", "A12", "B12"));
    values.sort(new AisleComparator(SortDirection.ASC));

    assertEquals(List.of("A12", "A12B", "B12"), values);
  }
}
