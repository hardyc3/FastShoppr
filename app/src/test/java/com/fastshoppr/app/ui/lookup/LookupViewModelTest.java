/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.fastshoppr.app.work.OrganizingScheduler;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public final class LookupViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private final ListItemRepository itemRepository = mock(ListItemRepository.class);
  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final SettingsRepository settingsRepository = mock(SettingsRepository.class);
  private final GetSelectedStoresUseCase getSelectedStores = mock(GetSelectedStoresUseCase.class);
  private final OrganizingScheduler scheduler = mock(OrganizingScheduler.class);
  private final OrganizingProgressStore progressStore = new OrganizingProgressStore();
  private final StoreContextFormatter formatter =
      new StoreContextFormatter() {
        @Override
        public String savedBranchesHeader() {
          return "Saved branches:";
        }

        @Override
        public String savedBranchLine(StoreBrand brand, String storeLabel) {
          return brand.getDisplayName() + ": " + storeLabel;
        }
      };

  private LookupViewModel viewModel;

  @Before
  public void setUp() {
    when(itemRepository.getByList("list-1"))
        .thenReturn(List.of(item("item-1", "store-1"), item("item-2", "store-2")));
    when(storeRepository.getById("store-1")).thenReturn(store());
    when(storeRepository.getById("store-2")).thenReturn(targetStore());
    when(getSelectedStores.execute()).thenReturn(Map.of());
    viewModel =
        new LookupViewModel(
            itemRepository,
            storeRepository,
            settingsRepository,
            getSelectedStores,
            formatter,
            scheduler,
            progressStore,
            DIRECT_EXECUTOR,
            new SavedStateHandle(Map.of(NavArguments.LIST_ID, "list-1")));
  }

  @Test
  public void loadContextAppendsSavedStoresForBrandsOnTheList() {
    NearbyStore walmart =
        new NearbyStore(
            "123",
            StoreBrand.WALMART,
            "Walmart Supercenter",
            "1 Main St",
            null,
            null,
            null,
            null,
            null,
            NearbyStoreSource.LOCATOR);
    NearbyStore target =
        new NearbyStore(
            "456",
            StoreBrand.TARGET,
            "Target #456",
            null,
            null,
            null,
            null,
            null,
            null,
            NearbyStoreSource.LOCATOR);
    NearbyStore lowes =
        new NearbyStore(
            "789",
            StoreBrand.LOWES,
            "Lowe's #789",
            null,
            null,
            null,
            null,
            null,
            null,
            NearbyStoreSource.LOCATOR);
    when(getSelectedStores.execute())
        .thenReturn(
            Map.of(
                StoreBrand.WALMART, walmart,
                StoreBrand.TARGET, target,
                StoreBrand.LOWES, lowes));

    viewModel.loadContext();

    String context = viewModel.getStoreContext().getValue();
    assertTrue(context.contains("Walmart: Walmart Supercenter — 1 Main St"));
    assertFalse(context.contains("Walmart — 1 Main St\n"));
    assertTrue(context.contains("Target: Target #456"));
    assertFalse(context.contains("Lowe's #789"));
  }

  @Test
  public void loadContextKeepsBaseStoreWhenNoBranchIsSaved() {
    viewModel.loadContext();

    assertEquals("Walmart — 1 Main St", viewModel.getStoreContext().getValue());
  }

  @Test
  public void presentBrandWithoutSavedBranchBlocksFirstUse() {
    viewModel.loadContext();

    assertEquals(StoreBrand.WALMART, viewModel.getMissingBranch().getValue());
    assertEquals(Boolean.FALSE, viewModel.getBranchesReady().getValue());
    assertEquals(Boolean.FALSE, viewModel.getBranchCheckDone().getValue());
  }

  @Test
  public void savedBranchesForPresentBrandsAllowFirstUse() {
    when(getSelectedStores.execute())
        .thenReturn(
            Map.of(
                StoreBrand.WALMART,
                branch(StoreBrand.WALMART),
                StoreBrand.TARGET,
                branch(StoreBrand.TARGET)));

    viewModel.loadContext();

    assertNull(viewModel.getMissingBranch().getValue());
    assertEquals(Boolean.TRUE, viewModel.getBranchesReady().getValue());
    assertEquals(Boolean.TRUE, viewModel.getBranchCheckDone().getValue());
  }

  @Test
  public void missingBranchFollowsLocatorBrandOrder() {
    when(itemRepository.getByList("list-1"))
        .thenReturn(List.of(item("item-1", "store-2"), item("item-2", "store-3")));
    when(storeRepository.getById("store-3")).thenReturn(homeDepotStore());

    viewModel.loadContext();

    assertEquals(StoreBrand.HOME_DEPOT, viewModel.getMissingBranch().getValue());
    assertEquals(Boolean.FALSE, viewModel.getBranchesReady().getValue());
  }

  @Test
  public void customStoreDoesNotRequireLocatorBranch() {
    when(itemRepository.getByList("list-1")).thenReturn(List.of(item("item-1", "store-custom")));
    when(storeRepository.getById("store-custom")).thenReturn(customStore());

    viewModel.loadContext();

    assertNull(viewModel.getMissingBranch().getValue());
    assertEquals(Boolean.TRUE, viewModel.getBranchesReady().getValue());
    assertEquals(Boolean.TRUE, viewModel.getBranchCheckDone().getValue());
  }

  private static NearbyStore branch(StoreBrand brand) {
    return new NearbyStore(
        brand.name(),
        brand,
        brand.getDisplayName() + " #123",
        "1 Main St",
        null,
        null,
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
  }

  private static Store homeDepotStore() {
    return new Store(
        "store-3",
        StoreBrand.HOME_DEPOT,
        "Home Depot",
        "3 Market St",
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        false,
        1L,
        1L);
  }

  private static Store customStore() {
    return new Store(
        "store-custom",
        StoreBrand.CUSTOM,
        "My shop",
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        false,
        1L,
        1L);
  }

  private static ListItem item(String id, String storeId) {
    return ListItem.builder()
        .id(id)
        .listId("list-1")
        .storeId(storeId)
        .rawText("milk")
        .displayName("milk")
        .normalizedText("milk")
        .targetDate(LocalDate.of(2026, 11, 20))
        .lookupStatus(LookupStatus.PENDING)
        .source(ItemSource.TYPED)
        .sortPosition(0)
        .createdAt(1L)
        .updatedAt(1L)
        .build();
  }

  private static Store store() {
    return new Store(
        "store-1",
        StoreBrand.WALMART,
        "Walmart",
        "1 Main St",
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        false,
        1L,
        1L);
  }

  private static Store targetStore() {
    return new Store(
        "store-2",
        StoreBrand.TARGET,
        "Target",
        "2 Market St",
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        false,
        1L,
        1L);
  }

  private static final AbstractExecutorService DIRECT_EXECUTOR =
      new AbstractExecutorService() {
        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
          return List.of();
        }

        @Override
        public boolean isShutdown() {
          return false;
        }

        @Override
        public boolean isTerminated() {
          return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
          return true;
        }

        @Override
        public void execute(Runnable command) {
          command.run();
        }
      };
}
