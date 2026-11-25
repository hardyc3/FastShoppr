/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.notification;

import com.fastshoppr.app.domain.model.ShoppingList;

/**
 * Posts the "list created" notification. Returns false when the notification could not be shown.
 */
public interface ListCreationNotifier {

  boolean notify(ShoppingList list);
}
