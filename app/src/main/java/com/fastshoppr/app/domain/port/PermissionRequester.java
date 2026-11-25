/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.port;

import com.fastshoppr.app.domain.model.PlatformPermission;
import java.util.function.Consumer;

/** Issues a runtime permission prompt and reports the user's decision. */
public interface PermissionRequester {

  void request(PlatformPermission permission, Consumer<Boolean> result);
}
