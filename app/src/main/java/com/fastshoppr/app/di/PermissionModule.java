/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.domain.port.PermissionRequester;
import com.fastshoppr.app.domain.port.PermissionStatus;
import com.fastshoppr.app.ui.common.PermissionLauncherBinder;
import com.fastshoppr.app.work.AndroidPermissionRequester;
import com.fastshoppr.app.work.AndroidPermissionStatus;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class PermissionModule {

  @Binds
  @Singleton
  abstract PermissionStatus bindPermissionStatus(AndroidPermissionStatus implementation);

  @Binds
  @Singleton
  abstract PermissionRequester bindPermissionRequester(AndroidPermissionRequester implementation);

  @Binds
  @Singleton
  abstract PermissionLauncherBinder bindPermissionLauncherBinder(
      AndroidPermissionRequester implementation);
}
