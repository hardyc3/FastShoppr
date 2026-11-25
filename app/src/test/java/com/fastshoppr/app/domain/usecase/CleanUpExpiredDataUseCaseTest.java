/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.testing.DirectExecutorService;
import com.fastshoppr.app.testing.FixedClock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class CleanUpExpiredDataUseCaseTest {

  @Test
  public void keepsInWindowDeletedRowsAndPurgesExpiredCacheWithFakeClock() {
    long now = 1_800_000_000_000L;
    FixedClock clock = new FixedClock(now);
    ShoppingListRepository listRepository = mock(ShoppingListRepository.class);
    AisleCacheRepository cacheRepository = mock(AisleCacheRepository.class);
    long outsideWindow = now - Duration.ofDays(31).toMillis();
    long insideWindow = now - Duration.ofDays(5).toMillis();
    List<Long> deletedRows = new ArrayList<>(List.of(outsideWindow, insideWindow));
    when(listRepository.purgeDeletedBefore(anyLong()))
        .thenAnswer(
            invocation -> {
              long cutoff = invocation.getArgument(0);
              int before = deletedRows.size();
              deletedRows.removeIf(deletedAt -> deletedAt < cutoff);
              return before - deletedRows.size();
            });

    PurgeDeletedUseCase purge =
        new PurgeDeletedUseCase(listRepository, clock, new DirectExecutorService());
    CleanUpExpiredDataUseCase maintenance =
        new CleanUpExpiredDataUseCase(purge, cacheRepository, clock, new DirectExecutorService());

    Integer purged = maintenance.run().join();

    assertEquals(Integer.valueOf(1), purged);
    assertEquals(List.of(insideWindow), deletedRows);
    verify(cacheRepository).deleteExpired(now);
  }
}
