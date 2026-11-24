/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** Branch fields read from a retailer's own locator/detail page. */
public record BranchPage(
    String name,
    String addressLine,
    String city,
    String state,
    String postalCode,
    String title,
    String bodyText) {}
