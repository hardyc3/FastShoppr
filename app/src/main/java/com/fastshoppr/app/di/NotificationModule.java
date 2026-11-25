/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.domain.notification.ListCreationNotifier;
import com.fastshoppr.app.domain.notification.NotificationPermissionStatus;
import com.fastshoppr.app.work.ScheduleNotificationHelper;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class NotificationModule {

  @Binds
  @Singleton
  abstract ListCreationNotifier bindListCreationNotifier(ScheduleNotificationHelper implementation);

  @Binds
  @Singleton
  abstract NotificationPermissionStatus bindNotificationPermissionStatus(
      ScheduleNotificationHelper.SystemPermissionStatus implementation);
}
