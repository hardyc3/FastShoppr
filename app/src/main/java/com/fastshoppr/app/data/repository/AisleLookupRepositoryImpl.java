/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import com.fastshoppr.app.data.remote.lookup.LookupQueryNormalizer;
import com.fastshoppr.app.data.remote.lookup.RateLimiter;
import com.fastshoppr.app.data.remote.lookup.StoreLookupMessages;
import com.fastshoppr.app.data.remote.lookup.StoreLookupSupport;
import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.lookup.LookupCandidatesCodec;
import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import com.fastshoppr.app.domain.model.AisleCache;
import com.fastshoppr.app.domain.model.ItemLocation;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.AisleLookupRepository.LookupOutcome;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class AisleLookupRepositoryImpl implements AisleLookupRepository {

  private static final long CACHE_TTL_MILLIS = 60L * 24L * 60L * 60L * 1000L;
  private static final int MAX_ATTEMPTS = 3; // initial + 2 retries for transient errors
  private static final long INITIAL_BACKOFF_MS = 500L;
  private static final long ITEM_DEADLINE_MILLIS = 30_000L;

  private final ListItemRepository itemRepository;
  private final AisleCacheRepository cacheRepository;
  private final StoreRepository storeRepository;
  private final Clock clock;
  private final IdGenerator idGenerator;
  private final RateLimiter rateLimiter;
  private final StoreLookupSupport lookupSupport;
  private final StoreLookupMessages messages;
  private final Map<StoreBrand, StoreLookupService> services;

  @Inject
  public AisleLookupRepositoryImpl(
      ListItemRepository itemRepository,
      AisleCacheRepository cacheRepository,
      StoreRepository storeRepository,
      Clock clock,
      IdGenerator idGenerator,
      RateLimiter rateLimiter,
      StoreLookupSupport lookupSupport,
      StoreLookupMessages messages,
      Set<StoreLookupService> serviceSet) {
    this.itemRepository = itemRepository;
    this.cacheRepository = cacheRepository;
    this.storeRepository = storeRepository;
    this.clock = clock;
    this.idGenerator = idGenerator;
    this.rateLimiter = rateLimiter;
    this.lookupSupport = lookupSupport;
    this.messages = messages;
    this.services = new EnumMap<>(StoreBrand.class);
    for (StoreLookupService service : serviceSet) {
      services.put(service.getBrand(), service);
    }
  }

  @Override
  public ListItem applyCandidate(String itemId, StoreLookupResult.Found candidate) {
    ListItem item = itemRepository.getById(itemId);
    if (item == null) {
      throw new IllegalArgumentException("Item not found: " + itemId);
    }
    if (item.getLookupSource() == LookupSource.MANUAL) {
      // a manual value the user entered always wins over lookup candidates
      return item;
    }
    long now = clock.nowMillis();
    ItemLocation loc = ItemLocation.of(candidate.getAisle(), candidate.getBay());
    ListItem updated =
        ListItem.builderFrom(item)
            .aisle(loc.aisle())
            .bay(loc.bay())
            .matchedProductName(candidate.getProductName())
            .matchedProductUrl(candidate.getProductUrl())
            .matchConfidence(candidate.getConfidence())
            .lookupStatus(LookupStatus.FOUND)
            .lookupSource(candidate.getSource())
            .lookupError(null)
            .matchCandidatesJson(null)
            .updatedAt(now)
            .build();
    itemRepository.update(updated);
    return updated;
  }

  @Override
  public AisleLookupRepository.LookupOutcome lookupItem(String itemId, boolean forceRefresh) {
    ListItem item = itemRepository.getById(itemId);
    if (item == null) {
      throw new IllegalArgumentException("Item not found: " + itemId);
    }
    if (item.getLookupSource() == LookupSource.MANUAL) {
      return LookupOutcome.unchanged(item, null);
    }

    long now = clock.nowMillis();
    Store store = storeRepository.getById(item.getStoreId());
    if (store == null) {
      return unresolvedStoreOutcome(item, now);
    }
    String query = normalizeQuery(item);

    if (!forceRefresh) {
      AisleCache cached = cacheRepository.find(store.getId(), query);
      if (cached != null && cacheMatchesBrand(cached, store)) {
        if (!cached.isExpired(now)) {
          ListItem updated = applyCacheHit(item, cached, false, now);
          itemRepository.update(updated);
          return LookupOutcome.fromCache(updated, cached, foundFromCache(cached));
        }
        // expired: usable offline with stale indicator; do not hit network from here when offline
        // caller/UI may treat as stale and optionally refresh. For an active lookup attempt,
        // we can try network and fall back to stale on network unavailability.
      }
    }

    StoreLookupSupport.Decision supportDecision = lookupSupport.forBrand(store.getBrand());
    if (supportDecision.mode == StoreLookupSupport.Mode.MANUAL_ONLY) {
      return LookupOutcome.unchanged(item, null);
    }
    if (supportDecision.mode != StoreLookupSupport.Mode.ENABLED) {
      String reason = messages.forBrand(store.getBrand());
      StoreLookupResult result =
          supportDecision.mode == StoreLookupSupport.Mode.BLOCKED
              ? new StoreLookupResult.Blocked(reason)
              : new StoreLookupResult.NotFound(reason);
      return applyNetworkResult(itemId, store, query, now, result);
    }

    StoreLookupService service = services.get(store.getBrand());
    if (service == null) {
      ListItem updated =
          updateItem(
              item,
              LookupStatus.ERROR,
              item.getLookupSource(),
              "Lookup not available for this store; enter aisle manually.",
              null,
              null,
              null,
              null,
              null,
              now);
      itemRepository.update(updated);
      return LookupOutcome.unchanged(
          updated, new StoreLookupResult.Error(new AppError.LookupNotFound("No lookup service")));
    }

    markInProgress(item, now);
    long deadline = now + ITEM_DEADLINE_MILLIS;
    StoreLookupResult result = performNetworkLookup(service, store, item, query, deadline);
    return applyNetworkResult(itemId, store, query, now, result);
  }

  private boolean cacheMatchesBrand(AisleCache cached, Store store) {
    Store cachedStore = storeRepository.getById(cached.getStoreId());
    if (cachedStore == null || cachedStore.getBrand() != store.getBrand()) {
      return false;
    }
    // A cache entry records the brand that produced it; never let another brand reuse it.
    return switch (cached.getSource()) {
      case WALMART_WEBVIEW -> store.getBrand() == StoreBrand.WALMART;
      case HOME_DEPOT_HTTP -> store.getBrand() == StoreBrand.HOME_DEPOT;
      case MANUAL, CACHE -> true;
    };
  }

  private AisleLookupRepository.LookupOutcome applyNetworkResult(
      String itemId, Store store, String query, long startNow, StoreLookupResult result) {
    long now = clock.nowMillis();
    ListItem item = itemRepository.getById(itemId);
    if (item == null || item.getLookupSource() == LookupSource.MANUAL) {
      return LookupOutcome.unchanged(item, result);
    }

    if (result instanceof StoreLookupResult.Found found) {
      AisleCache cache = toCache(store.getId(), query, found, now);
      cacheRepository.put(cache);
      ListItem updated = applyFound(item, found, now);
      itemRepository.update(updated);
      return LookupOutcome.fromNetwork(updated, cache, result);
    }

    if (result instanceof StoreLookupResult.Multiple multiple) {
      ListItem updated =
          ListItem.builderFrom(item)
              .lookupStatus(LookupStatus.NEEDS_REVIEW)
              .lookupSource(null)
              .lookupError(
                  "Multiple matches found. Choose the right product or enter aisle manually.")
              .matchCandidatesJson(LookupCandidatesCodec.encode(multiple.getCandidates()))
              .updatedAt(now)
              .build();
      itemRepository.update(updated);
      return LookupOutcome.unchanged(updated, result);
    }

    if (result instanceof StoreLookupResult.NotFound notFound) {
      ListItem updated =
          updateItem(
              item,
              LookupStatus.NOT_FOUND,
              null,
              safeMessage(notFound.getReason(), "No in-store aisle found."),
              null,
              null,
              null,
              null,
              null,
              now);
      itemRepository.update(updated);
      return LookupOutcome.unchanged(updated, result);
    }

    if (result instanceof StoreLookupResult.Blocked blocked) {
      String msg = "Blocked: " + safeMessage(blocked.getReason(), "Store access blocked.");
      ListItem updated =
          updateItem(item, LookupStatus.ERROR, null, msg, null, null, null, null, null, now);
      itemRepository.update(updated);
      return LookupOutcome.blocked(updated, result);
    }

    if (result instanceof StoreLookupResult.Error err) {
      AisleCache stale = cacheRepository.find(store.getId(), query);
      boolean staleUsable = stale != null && stale.isExpired(now) && isOfflineError(err.getError());
      if (staleUsable) {
        ListItem updated = applyCacheHit(item, stale, true, now);
        itemRepository.update(updated);
        return LookupOutcome.fromStaleCache(updated, stale, result);
      }
      ListItem updated =
          updateItem(
              item,
              LookupStatus.ERROR,
              null,
              messageForError(err.getError()),
              null,
              null,
              null,
              null,
              null,
              now);
      itemRepository.update(updated);
      return LookupOutcome.unchanged(updated, result);
    }

    ListItem updated =
        updateItem(
            item, LookupStatus.ERROR, null, "Lookup failed.", null, null, null, null, null, now);
    itemRepository.update(updated);
    return LookupOutcome.unchanged(updated, result);
  }

  private AisleLookupRepository.LookupOutcome unresolvedStoreOutcome(ListItem item, long now) {
    ListItem updated =
        updateItem(
            item,
            LookupStatus.ERROR,
            item.getLookupSource(),
            "Store no longer exists; choose a store and try again.",
            null,
            null,
            null,
            null,
            null,
            now);
    itemRepository.update(updated);
    return LookupOutcome.unchanged(
        updated, new StoreLookupResult.Error(new AppError.LookupNotFound("Unknown store")));
  }

  private StoreLookupResult performNetworkLookup(
      StoreLookupService service, Store store, ListItem item, String primaryQuery, long deadline) {
    List<String> queries = LookupQueryNormalizer.alternates(item.getDisplayName());
    if (queries.isEmpty()) {
      queries = List.of(primaryQuery);
    }
    StoreLookupResult last = new StoreLookupResult.NotFound("No query");
    for (int i = 0; i < queries.size(); i++) {
      if (clock.nowMillis() >= deadline) {
        return new StoreLookupResult.Error(
            new AppError.NetworkUnavailable("Lookup exceeded the per-item time limit."));
      }
      String q = queries.get(i);
      last = lookupWithRetries(service, store, item, q, deadline);
      if (last instanceof StoreLookupResult.Found
          || last instanceof StoreLookupResult.Multiple
          || last instanceof StoreLookupResult.Blocked) {
        return last;
      }
      // Alternate queries only after no result.
      if (!(last instanceof StoreLookupResult.NotFound)) {
        return last;
      }
    }
    return last;
  }

  private StoreLookupResult lookupWithRetries(
      StoreLookupService service, Store store, ListItem item, String query, long deadline) {
    StoreLookupResult last = null;
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      if (clock.nowMillis() >= deadline) {
        return new StoreLookupResult.Error(
            new AppError.NetworkUnavailable("Lookup exceeded the per-item time limit."));
      }
      try {
        rateLimiter.acquire(store.getBrand());
        StoreLookupRequest request =
            new StoreLookupRequest(item.getRawText(), item.getDisplayName(), query, store);
        long waitMs = Math.max(1L, deadline - clock.nowMillis());
        last = service.lookup(request).get(waitMs, TimeUnit.MILLISECONDS);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        last = new StoreLookupResult.Error(new AppError.NetworkUnavailable(cause.getMessage()));
      } catch (TimeoutException e) {
        last = new StoreLookupResult.Error(new AppError.NetworkUnavailable("Lookup timed out."));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return new StoreLookupResult.Error(new AppError.UnknownError("Lookup interrupted."));
      } catch (RuntimeException e) {
        last = new StoreLookupResult.Error(new AppError.UnknownError(e.getMessage()));
      }

      if (last instanceof StoreLookupResult.Blocked) {
        return last; // do not retry blocked
      }
      if (last instanceof StoreLookupResult.Error err && isTransient(err.getError())) {
        if (attempt < MAX_ATTEMPTS - 1) {
          sleepBackoff(attempt);
          continue;
        }
        return last;
      }
      return last == null
          ? new StoreLookupResult.Error(new AppError.UnknownError("No result"))
          : last;
    }
    return last == null
        ? new StoreLookupResult.Error(new AppError.UnknownError("No result"))
        : last;
  }

  private void sleepBackoff(int attempt) {
    long delay = INITIAL_BACKOFF_MS * (1L << attempt);
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean isTransient(AppError error) {
    return error instanceof AppError.NetworkUnavailable || error instanceof AppError.RateLimited;
  }

  private boolean isOfflineError(AppError error) {
    return error instanceof AppError.NetworkUnavailable;
  }

  private void markInProgress(ListItem item, long now) {
    ListItem updated =
        ListItem.builderFrom(item)
            .lookupStatus(LookupStatus.IN_PROGRESS)
            .lookupError(null)
            .updatedAt(now)
            .build();
    itemRepository.update(updated);
  }

  private ListItem applyCacheHit(ListItem item, AisleCache cache, boolean stale, long now) {
    LookupStatus status = LookupStatus.FOUND;
    String error = stale ? "Showing saved aisle (may be outdated)." : null;
    return ListItem.builderFrom(item)
        .aisle(cache.getAisle())
        .matchCandidatesJson(null)
        .bay(cache.getBay())
        .matchedProductName(cache.getMatchedProductName())
        .matchedProductUrl(cache.getMatchedProductUrl())
        .matchConfidence(cache.getConfidence())
        .lookupStatus(status)
        .lookupSource(LookupSource.CACHE)
        .lookupError(error)
        .updatedAt(now)
        .build();
  }

  private ListItem applyFound(ListItem item, StoreLookupResult.Found found, long now) {
    boolean lowConfidence = found.getConfidence() != null && found.getConfidence() < 0.6f;
    LookupStatus status = lowConfidence ? LookupStatus.NEEDS_REVIEW : LookupStatus.FOUND;
    String error =
        lowConfidence ? "Low confidence match. Confirm the aisle or choose another product." : null;
    ItemLocation loc = ItemLocation.of(found.getAisle(), found.getBay());
    return ListItem.builderFrom(item)
        .aisle(loc.aisle())
        .bay(loc.bay())
        .matchCandidatesJson(null)
        .matchedProductName(found.getProductName())
        .matchedProductUrl(found.getProductUrl())
        .matchConfidence(found.getConfidence())
        .lookupStatus(status)
        .lookupSource(found.getSource())
        .lookupError(error)
        .updatedAt(now)
        .build();
  }

  private ListItem updateItem(
      ListItem item,
      LookupStatus status,
      LookupSource source,
      String error,
      String aisle,
      String bay,
      String matchedName,
      String matchedUrl,
      Float confidence,
      long now) {
    // Preserve previously known aisle/match values on failure outcomes; only replace them
    // when the caller provides a value (a successful lookup or explicit clear).
    ListItem.Builder builder = ListItem.builderFrom(item);
    if (aisle != null) {
      builder.aisle(aisle);
    }
    if (bay != null) {
      builder.bay(bay);
    }
    if (matchedName != null) {
      builder.matchedProductName(matchedName);
    }
    if (matchedUrl != null) {
      builder.matchedProductUrl(matchedUrl);
    }
    if (confidence != null) {
      builder.matchConfidence(confidence);
    }
    return builder
        .lookupStatus(status)
        .lookupSource(source)
        .lookupError(error)
        .matchCandidatesJson(null)
        .updatedAt(now)
        .build();
  }

  private AisleCache toCache(
      String storeId, String query, StoreLookupResult.Found found, long now) {
    return new AisleCache(
        idGenerator.newId(),
        storeId,
        query,
        found.getAisle(),
        found.getBay(),
        found.getProductName(),
        found.getProductUrl(),
        found.getConfidence(),
        found.getSource() == null ? LookupSource.CACHE : found.getSource(),
        now,
        now + CACHE_TTL_MILLIS);
  }

  private StoreLookupResult foundFromCache(AisleCache cache) {
    return new StoreLookupResult.Found(
        cache.getAisle(),
        cache.getBay(),
        cache.getMatchedProductName(),
        cache.getMatchedProductUrl(),
        cache.getConfidence(),
        LookupSource.CACHE);
  }

  private String normalizeQuery(ListItem item) {
    String q = item.getNormalizedText();
    if (q == null || q.trim().isEmpty()) {
      q = LookupQueryNormalizer.normalize(item.getDisplayName());
    }
    return q;
  }

  private static String safeMessage(String value, String fallback) {
    if (value == null || value.trim().isEmpty()) {
      return fallback;
    }
    return value.trim();
  }

  private static String messageForError(AppError error) {
    if (error instanceof AppError.NetworkUnavailable) {
      return "Network unavailable. Try again when connected.";
    }
    if (error instanceof AppError.RateLimited) {
      return "Rate limited. Try again later.";
    }
    if (error instanceof AppError.AccessBlocked) {
      return "Blocked: Store access blocked.";
    }
    if (error instanceof AppError.ParserFailed) {
      return "Could not read store page. Enter aisle manually.";
    }
    if (error instanceof AppError.LookupNotFound n) {
      return safeMessage(n.message(), "No aisle found for this item.");
    }
    if (error instanceof AppError.UnknownError u) {
      return safeMessage(u.message(), "Lookup failed.");
    }
    return "Lookup failed.";
  }
}
