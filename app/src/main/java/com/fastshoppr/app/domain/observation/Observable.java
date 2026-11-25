/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.observation;

import java.util.function.Consumer;

/** A framework-free stream of values. Subscribers receive the current value immediately. */
public interface Observable<T> {

  Registration subscribe(Consumer<T> observer);
}
