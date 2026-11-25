/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import com.fastshoppr.app.data.remote.lookup.RateLimiter;
import com.fastshoppr.app.data.remote.lookup.StoreLookupSupport;
import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import com.fastshoppr.app.domain.model.AisleCache;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.observation.Observable;
import com.fastshoppr.app.domain.observation.Registration;
import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.FixedClock;
import com.fastshoppr.app.testing.FixedIdGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AisleLookupRepositoryImplTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private FixedClock clock;
  private FakeItemRepository itemRepo;
  private FakeCacheRepository cacheRepo;
  private FakeStoreRepository storeRepo;
  private CountingService walmartService;
  private AisleLookupRepositoryImpl repo;
  private Store walmartStore;

  @Before
  public void setUp() {
    clock = new FixedClock(1_000_000L);
    itemRepo = new FakeItemRepository();
    cacheRepo = new FakeCacheRepository();
    storeRepo = new FakeStoreRepository();
    walmartService = new CountingService(StoreBrand.WALMART);
    walmartStore =
        new Store(
            "store-wm",
            StoreBrand.WALMART,
            "Walmart",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            true,
            false,
            0L,
            0L);
    storeRepo.put(walmartStore);
    repo =
        new AisleLookupRepositoryImpl(
            itemRepo,
            cacheRepo,
            storeRepo,
            clock,
            new FixedIdGenerator(),
            new RateLimiter(clock),
            new StoreLookupSupport(),
            Set.of(walmartService));
  }

  @Test
  public void observableDeliversCurrentValueAndStopsAfterClose() {
    MutableLiveData<String> source = new MutableLiveData<>("initial");
    Observable<String> observable = new LiveDataObservable<>(source);
    List<String> values = new ArrayList<>();

    Registration registration = observable.subscribe(values::add);
    assertEquals(List.of("initial"), values);
    source.setValue("next");
    assertEquals(List.of("initial", "next"), values);

    registration.close();
    source.setValue("after-close");
    assertEquals(2, values.size());
  }

  @Test
  public void cacheHitAvoidsNetwork() {
    cacheRepo.put(
        new AisleCache(
            "c1",
            walmartStore.getId(),
            "milk",
            "A1",
            null,
            "Milk",
            null,
            0.9f,
            LookupSource.WALMART_WEBVIEW,
            clock.nowMillis(),
            clock.nowMillis() + 10_000L));
    ListItem item = item("i1", "milk", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i1", false);

    assertTrue(outcome.fromCache);
    assertFalse(outcome.stale);
    assertEquals(0, walmartService.calls.get());
    assertEquals(LookupStatus.FOUND, outcome.item.getLookupStatus());
    assertEquals(LookupSource.CACHE, outcome.item.getLookupSource());
    assertEquals("A1", outcome.item.getAisle());
  }

  @Test
  public void manualPrecedenceSkipsLookup() {
    ListItem item = item("i2", "milk", LookupStatus.FOUND, LookupSource.MANUAL);
    item = ListItem.builderFrom(item).aisle("M1").build();
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i2", true);

    assertEquals(0, walmartService.calls.get());
    assertEquals(LookupSource.MANUAL, outcome.item.getLookupSource());
    assertEquals("M1", outcome.item.getAisle());
  }

  @Test
  public void blockedResponseStopsAndMarksErrorWithoutRetry() {
    walmartService.result = new StoreLookupResult.Blocked("blocked");
    ListItem item = item("i3", "eggs", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i3", false);

    assertTrue(outcome.blocked);
    assertEquals(1, walmartService.calls.get());
    assertEquals(LookupStatus.ERROR, outcome.item.getLookupStatus());
    assertTrue(outcome.item.getLookupError().contains("Blocked"));
  }

  @Test
  public void alternateQueriesUsedAfterNoResult() {
    walmartService.results = new ArrayList<>();
    walmartService.results.add(new StoreLookupResult.NotFound("no"));
    walmartService.results.add(
        new StoreLookupResult.Found(
            "B2", null, "2% Milk", null, 0.9f, LookupSource.WALMART_WEBVIEW));
    ListItem item = item("i4", "2 gallons milk", LookupStatus.PENDING, null);
    item =
        ListItem.builderFrom(item)
            .displayName("2 gallons milk")
            .normalizedText("2 gallons milk")
            .build();
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i4", false);

    assertEquals(2, walmartService.calls.get());
    assertEquals(LookupStatus.FOUND, outcome.item.getLookupStatus());
    assertEquals("B2", outcome.item.getAisle());
  }

  @Test
  public void staleCacheUsableOnNetworkUnavailable() {
    cacheRepo.put(
        new AisleCache(
            "c2",
            walmartStore.getId(),
            "bread",
            "C3",
            null,
            "Bread",
            null,
            0.8f,
            LookupSource.WALMART_WEBVIEW,
            clock.nowMillis() - 100_000L,
            clock.nowMillis() - 1L)); // expired
    walmartService.result = new StoreLookupResult.Error(new AppError.NetworkUnavailable("offline"));
    ListItem item = item("i5", "bread", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i5", false);

    assertTrue(outcome.fromCache);
    assertTrue(outcome.stale);
    assertEquals(LookupSource.CACHE, outcome.item.getLookupSource());
    assertEquals("C3", outcome.item.getAisle());
    assertNotNull(outcome.item.getLookupError());
  }

  @Test
  public void errorAndBlockedPreserveKnownAisle() {
    walmartService.result = new StoreLookupResult.Blocked("blocked");
    ListItem item = item("i6", "eggs", LookupStatus.FOUND, LookupSource.WALMART_WEBVIEW);
    item = ListItem.builderFrom(item).aisle("B1").matchedProductName("Large Eggs").build();
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome blockedOutcome = repo.lookupItem("i6", true);
    assertEquals("B1", blockedOutcome.item.getAisle());
    assertEquals("Large Eggs", blockedOutcome.item.getMatchedProductName());
    assertEquals(LookupStatus.ERROR, blockedOutcome.item.getLookupStatus());

    walmartService.result = new StoreLookupResult.Error(new AppError.NetworkUnavailable("down"));
    // no expired cache present, so the error outcome stands; aisle must still be preserved
    AisleLookupRepository.LookupOutcome errorOutcome = repo.lookupItem("i6", true);
    assertEquals("B1", errorOutcome.item.getAisle());
    assertEquals("Large Eggs", errorOutcome.item.getMatchedProductName());
    assertEquals(LookupStatus.ERROR, errorOutcome.item.getLookupStatus());
  }

  @Test
  public void applyCandidateDoesNotOverwriteManualValue() {
    ListItem item = item("i7", "eggs", LookupStatus.FOUND, LookupSource.MANUAL);
    item = ListItem.builderFrom(item).aisle("M1").build();
    itemRepo.insert(item);

    ListItem updated =
        repo.applyCandidate(
            "i7",
            new StoreLookupResult.Found(
                "C2",
                "003",
                "Eggs",
                "https://www.walmart.com/ip/eggs/1",
                0.9f,
                LookupSource.WALMART_WEBVIEW));

    assertEquals("M1", updated.getAisle());
    assertEquals(LookupSource.MANUAL, updated.getLookupSource());
  }

  @Test
  public void applyCandidateConfirmsPickedMatch() {
    ListItem item = item("i8", "eggs", LookupStatus.NEEDS_REVIEW, LookupSource.WALMART_WEBVIEW);
    itemRepo.insert(item);

    ListItem updated =
        repo.applyCandidate(
            "i8",
            new StoreLookupResult.Found(
                "C2",
                "003",
                "Eggs",
                "https://www.walmart.com/ip/eggs/1",
                0.5f,
                LookupSource.WALMART_WEBVIEW));

    assertEquals(LookupStatus.FOUND, updated.getLookupStatus());
    assertNull(updated.getLookupError());
    assertEquals("C2", updated.getAisle());
    assertEquals("003", updated.getBay());
    assertEquals("Eggs", updated.getMatchedProductName());
  }

  @Test
  public void transientErrorIsRetriedTwiceThenGivesUp() {
    TransientErrorService service = new TransientErrorService();
    repo =
        new AisleLookupRepositoryImpl(
            itemRepo,
            cacheRepo,
            storeRepo,
            clock,
            new FixedIdGenerator(),
            new RateLimiter(clock),
            new StoreLookupSupport(),
            Set.of(service));
    ListItem item = item("i9", "eggs", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i9", true);

    assertEquals(3, service.calls.get()); // initial + 2 retries (MAX_ATTEMPTS)
    assertEquals(LookupStatus.ERROR, outcome.item.getLookupStatus());
  }

  @Test
  public void blockedIsNeverRetried() {
    walmartService.result = new StoreLookupResult.Blocked("blocked");
    ListItem item = item("i10", "eggs", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    repo.lookupItem("i10", true);

    assertEquals(1, walmartService.calls.get());
  }

  @Test
  public void multipleResultPersistsCandidatesForRowPicker() {
    walmartService.result =
        new StoreLookupResult.Multiple(
            List.of(
                new StoreLookupResult.Found(
                    "A1", null, "Milk A", null, 0.7f, LookupSource.WALMART_WEBVIEW),
                new StoreLookupResult.Found(
                    "A2", null, "Milk B", null, 0.6f, LookupSource.WALMART_WEBVIEW)));
    ListItem item = item("i11", "milk", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i11", true);

    assertEquals(LookupStatus.NEEDS_REVIEW, outcome.item.getLookupStatus());
    List<StoreLookupResult.Found> candidates =
        com.fastshoppr.app.domain.lookup.LookupCandidatesCodec.decode(
            outcome.item.getMatchCandidatesJson());
    assertEquals(2, candidates.size());
    assertEquals("A2", candidates.get(1).getAisle());
  }

  @Test
  public void foundAfterMultipleClearsPersistedCandidates() {
    walmartService.results = new ArrayList<>();
    walmartService.results.add(
        new StoreLookupResult.Multiple(
            List.of(
                new StoreLookupResult.Found(
                    "A1", null, "Milk A", null, 0.7f, LookupSource.WALMART_WEBVIEW))));
    ListItem item = item("i12", "milk", LookupStatus.PENDING, null);
    itemRepo.insert(item);
    repo.lookupItem("i12", true);
    assertNotNull(itemRepo.getById("i12").getMatchCandidatesJson());

    walmartService.results = new ArrayList<>();
    walmartService.results.add(
        new StoreLookupResult.Found("A9", null, "Milk", null, 0.9f, LookupSource.WALMART_WEBVIEW));
    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i12", true);

    assertEquals(LookupStatus.FOUND, outcome.item.getLookupStatus());
    assertEquals(null, outcome.item.getMatchCandidatesJson());
  }

  @Test
  public void candidatesPersistAcrossCacheMiss() {
    // cache hit path must not keep stale candidate payloads either
    cacheRepo.put(
        new AisleCache(
            "c9",
            walmartStore.getId(),
            "milk",
            "B7",
            null,
            "Milk",
            null,
            0.9f,
            LookupSource.WALMART_WEBVIEW,
            clock.nowMillis(),
            clock.nowMillis() + 10_000L));
    ListItem item = item("i13", "milk", LookupStatus.NEEDS_REVIEW, LookupSource.WALMART_WEBVIEW);
    item = ListItem.builderFrom(item).matchCandidatesJson("[{\"aisle\":\"A1\"}]").build();
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i13", false);

    assertTrue(outcome.fromCache);
    assertEquals(null, outcome.item.getMatchCandidatesJson());
  }

  @Test
  public void blockedBrandStopsWithoutCallingItsService() {
    Store lowes = store(StoreBrand.LOWES, "store-lowes");
    storeRepo.put(lowes);
    CountingService lowesService = new CountingService(StoreBrand.LOWES);
    repo =
        new AisleLookupRepositoryImpl(
            itemRepo,
            cacheRepo,
            storeRepo,
            clock,
            new FixedIdGenerator(),
            new RateLimiter(clock),
            new StoreLookupSupport(),
            Set.of(walmartService, lowesService));
    ListItem item = itemAtStore("i-lowes", "paper towels", lowes.getId());
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-lowes", true);

    assertTrue(outcome.blocked);
    assertTrue(outcome.result instanceof StoreLookupResult.Blocked);
    assertEquals(
        "Lowe's access is denied; enter aisle manually.",
        ((StoreLookupResult.Blocked) outcome.result).getReason());
    assertEquals(LookupStatus.ERROR, outcome.item.getLookupStatus());
    assertEquals(0, lowesService.calls.get());
  }

  @Test
  public void smithsWithoutPublicAisleReturnsNotFound() {
    Store smiths = store(StoreBrand.SMITHS, "store-smiths");
    storeRepo.put(smiths);
    ListItem item = itemAtStore("i-smiths", "milk", smiths.getId());
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-smiths", true);

    assertTrue(outcome.result instanceof StoreLookupResult.NotFound);
    assertEquals(LookupStatus.NOT_FOUND, outcome.item.getLookupStatus());
    assertEquals(
        "Smiths product pages do not show a public aisle; enter aisle manually.",
        outcome.item.getLookupError());
  }

  @Test
  public void customStoreKeepsManualOnlyItemUnchanged() {
    Store custom = store(StoreBrand.CUSTOM, "store-custom");
    storeRepo.put(custom);
    ListItem item = itemAtStore("i-custom", "milk", custom.getId());
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-custom", true);

    assertFalse(outcome.blocked);
    assertNull(outcome.result);
    assertEquals(LookupStatus.PENDING, outcome.item.getLookupStatus());
  }

  @Test
  public void cachedAisleIsReusedBeforeBrandBlock() {
    Store lowes = store(StoreBrand.LOWES, "store-lowes-cache");
    storeRepo.put(lowes);
    cacheRepo.put(
        new AisleCache(
            "c-lowes",
            lowes.getId(),
            "paper towels",
            "12",
            null,
            "Paper Towels",
            null,
            0.9f,
            LookupSource.MANUAL,
            clock.nowMillis(),
            clock.nowMillis() + 10_000L));
    ListItem item = itemAtStore("i-lowes-cache", "paper towels", lowes.getId());
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-lowes-cache", false);

    assertTrue(outcome.fromCache);
    assertFalse(outcome.blocked);
    assertEquals("12", outcome.item.getAisle());
  }

  @Test
  public void foundResultIsCachedForSixtyDays() {
    ListItem item = item("i-ttl", "milk", LookupStatus.PENDING, null);
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-ttl", false);

    assertEquals(
        clock.nowMillis() + 60L * 24L * 60L * 60L * 1000L, outcome.cacheEntry.getExpiresAt());
    assertFalse(outcome.cacheEntry.isExpired(clock.nowMillis() + 59L * 24L * 60L * 60L * 1000L));
    assertTrue(outcome.cacheEntry.isExpired(clock.nowMillis() + 61L * 24L * 60L * 60L * 1000L));
  }

  @Test
  public void cacheIsScopedToStoreBrand() {
    cacheRepo.put(
        new AisleCache(
            "c-wm",
            walmartStore.getId(),
            "paper towels",
            "A1",
            null,
            "Paper Towels",
            null,
            0.9f,
            LookupSource.WALMART_WEBVIEW,
            clock.nowMillis(),
            clock.nowMillis() + 10_000L));
    Store lowes = store(StoreBrand.LOWES, "store-lowes-separate");
    storeRepo.put(lowes);
    ListItem item = itemAtStore("i-separate", "paper towels", lowes.getId());
    itemRepo.insert(item);

    AisleLookupRepository.LookupOutcome outcome = repo.lookupItem("i-separate", false);

    assertFalse(outcome.fromCache);
    assertTrue(outcome.blocked);
  }

  private Store store(StoreBrand brand, String id) {
    return new Store(
        id, brand, brand.name(), null, null, null, null, null, null, false, true, false, 0L, 0L);
  }

  private ListItem item(String id, String text, LookupStatus status, LookupSource source) {
    return ListItem.builder()
        .id(id)
        .listId("list-1")
        .storeId(walmartStore.getId())
        .rawText(text)
        .displayName(text)
        .normalizedText(text)
        .targetDate(LocalDate.of(2026, 11, 12))
        .lookupStatus(status)
        .lookupSource(source)
        .source(ItemSource.TYPED)
        .sortPosition(0)
        .createdAt(1L)
        .updatedAt(1L)
        .build();
  }

  private ListItem itemAtStore(String id, String text, String storeId) {
    return ListItem.builder()
        .id(id)
        .listId("list-1")
        .storeId(storeId)
        .rawText(text)
        .displayName(text)
        .normalizedText(text)
        .targetDate(LocalDate.of(2026, 11, 12))
        .lookupStatus(LookupStatus.PENDING)
        .source(ItemSource.TYPED)
        .sortPosition(0)
        .createdAt(1L)
        .updatedAt(1L)
        .build();
  }

  static class CountingService implements StoreLookupService {
    final StoreBrand brand;
    final AtomicInteger calls = new AtomicInteger();
    StoreLookupResult result =
        new StoreLookupResult.Found("A1", null, "P", null, 0.9f, LookupSource.WALMART_WEBVIEW);
    List<StoreLookupResult> results;

    CountingService(StoreBrand brand) {
      this.brand = brand;
    }

    @Override
    public StoreBrand getBrand() {
      return brand;
    }

    @Override
    public CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request) {
      int i = calls.getAndIncrement();
      if (results != null && i < results.size()) {
        return CompletableFuture.completedFuture(results.get(i));
      }
      if (results != null && !results.isEmpty()) {
        return CompletableFuture.completedFuture(results.get(results.size() - 1));
      }
      return CompletableFuture.completedFuture(result);
    }
  }

  static class TransientErrorService implements StoreLookupService {
    final AtomicInteger calls = new AtomicInteger();

    @Override
    public StoreBrand getBrand() {
      return StoreBrand.WALMART;
    }

    @Override
    public CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request) {
      calls.incrementAndGet();
      return CompletableFuture.completedFuture(
          new StoreLookupResult.Error(new AppError.NetworkUnavailable("transient")));
    }
  }

  static class FakeItemRepository implements ListItemRepository {
    final Map<String, ListItem> items = new HashMap<>();

    @Override
    public Observable<List<ListItem>> observeByList(String listId) {
      return observer -> {
        observer.accept(new ArrayList<>(items.values()));
        return () -> {};
      };
    }

    @Override
    public Observable<List<ListItem>> observeByListAndStore(String listId, String storeId) {
      return observer -> {
        observer.accept(List.of());
        return () -> {};
      };
    }

    @Override
    public Observable<ListItem> observeById(String id) {
      return observer -> {
        observer.accept(items.get(id));
        return () -> {};
      };
    }

    @Override
    public List<ListItem> getByList(String listId) {
      return new ArrayList<>(items.values());
    }

    @Override
    public ListItem getById(String id) {
      return items.get(id);
    }

    @Override
    public int nextSortPosition(String listId, String storeId) {
      return 0;
    }

    @Override
    public void insert(ListItem item) {
      items.put(item.getId(), item);
    }

    @Override
    public void update(ListItem item) {
      items.put(item.getId(), item);
    }

    @Override
    public void setChecked(String id, boolean checked) {}

    @Override
    public void softDelete(String id) {}

    @Override
    public ListItem moveToDate(String itemId, LocalDate targetDate, String targetListName) {
      return null;
    }

    @Override
    public int moveUncheckedItemsToList(
        String sourceListId, String targetListId, LocalDate targetDate) {
      return 0;
    }

    @Override
    public int purgeDeletedBefore(long cutoffMillis) {
      return 0;
    }
  }

  static class FakeCacheRepository implements AisleCacheRepository {
    final Map<String, AisleCache> byKey = new HashMap<>();

    private String key(String storeId, String q) {
      return storeId + "|" + q;
    }

    @Override
    public AisleCache find(String storeId, String normalizedQuery) {
      return byKey.get(key(storeId, normalizedQuery));
    }

    @Override
    public void put(AisleCache cache) {
      byKey.put(key(cache.getStoreId(), cache.getNormalizedQuery()), cache);
    }

    @Override
    public void delete(String id) {}

    @Override
    public void deleteByStore(String storeId) {}

    @Override
    public int deleteExpired(long nowMillis) {
      return 0;
    }
  }

  static class FakeStoreRepository implements StoreRepository {
    final Map<String, Store> stores = new HashMap<>();

    void put(Store s) {
      stores.put(s.getId(), s);
    }

    @Override
    public Observable<List<Store>> observeActive() {
      return observer -> {
        observer.accept(new ArrayList<>(stores.values()));
        return () -> {};
      };
    }

    @Override
    public List<Store> getActive() {
      return new ArrayList<>(stores.values());
    }

    @Override
    public Store getById(String id) {
      return stores.get(id);
    }

    @Override
    public Store findByBrand(StoreBrand brand) {
      return null;
    }

    @Override
    public Store getDefault() {
      return null;
    }

    @Override
    public void save(Store store) {
      stores.put(store.getId(), store);
    }

    @Override
    public void setDefault(String id) {}

    @Override
    public void archive(String id) {}

    @Override
    public void restore(String id) {}

    @Override
    public void delete(String id) {}
  }
}
