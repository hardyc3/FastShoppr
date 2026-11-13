/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CleanItemResult;

/** Cleanup result plus the cleaner that produced it. */
public record CleanupOutcome(CleanItemResult result, AiCleanupSource source) {}
