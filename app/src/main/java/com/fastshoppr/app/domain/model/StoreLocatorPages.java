/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Public retailer locator pages that may be shown in the branch picker WebView. */
public final class StoreLocatorPages {

  private static final List<StoreBrand> ORDER =
      List.of(
          StoreBrand.WALMART,
          StoreBrand.HOME_DEPOT,
          StoreBrand.LOWES,
          StoreBrand.TARGET,
          StoreBrand.SMITHS);

  private static final Set<StoreBrand> SUPPORTED = Set.copyOf(ORDER);

  private StoreLocatorPages() {}

  public static Set<StoreBrand> supportedBrands() {
    return SUPPORTED;
  }

  public static List<StoreBrand> brandOrder() {
    return ORDER;
  }

  public static boolean supports(StoreBrand brand) {
    return brand != null && SUPPORTED.contains(brand);
  }

  public static String locatorUrl(StoreBrand brand) {
    if (!supports(brand)) {
      throw new IllegalArgumentException("No public locator page for brand: " + brand);
    }
    return switch (brand) {
      case WALMART -> "https://www.walmart.com/store/finder";
      case HOME_DEPOT -> "https://www.homedepot.com/l/store-locator";
      case LOWES -> "https://www.lowes.com/store-locator";
      case TARGET -> "https://www.target.com/store-locator";
      case SMITHS -> "https://www.smithsfoodanddrug.com/stores/search";
      default -> throw new IllegalArgumentException("No public locator page for brand: " + brand);
    };
  }

  /** Only HTTPS navigation on the selected retailer's own hosts may remain in the WebView. */
  public static boolean isAllowedNavigation(StoreBrand brand, String url) {
    if (!supports(brand) || url == null) {
      return false;
    }
    if ("about:blank".equals(url)) {
      return true;
    }
    try {
      URI uri = URI.create(url);
      if (!"https".equalsIgnoreCase(uri.getScheme())) {
        return false;
      }
      String host = uri.getHost();
      if (host == null) {
        return false;
      }
      host = host.toLowerCase(Locale.US);
      return switch (brand) {
        case WALMART -> host.equals("walmart.com") || host.endsWith(".walmart.com");
        case HOME_DEPOT -> host.equals("homedepot.com") || host.endsWith(".homedepot.com");
        case LOWES -> host.equals("lowes.com") || host.endsWith(".lowes.com");
        case TARGET -> host.equals("target.com") || host.endsWith(".target.com");
        case SMITHS ->
            host.equals("smithsfoodanddrug.com") || host.endsWith(".smithsfoodanddrug.com");
        default -> false;
      };
    } catch (IllegalArgumentException malformed) {
      return false;
    }
  }

  /** Detects retailer bot walls so the picker never asks the user to confirm a challenge page. */
  public static boolean isChallengePage(String title, String bodyText) {
    String text =
        ((title == null ? "" : title) + " " + (bodyText == null ? "" : bodyText))
            .toLowerCase(Locale.US);
    return text.contains("robot or human")
        || text.contains("are you a human")
        || text.contains("bot check")
        || text.contains("access denied")
        || text.contains("request blocked");
  }
}
