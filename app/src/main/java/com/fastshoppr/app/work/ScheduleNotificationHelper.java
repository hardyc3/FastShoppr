/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.fastshoppr.app.MainActivity;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ShoppingList;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ScheduleNotificationHelper {

  @Inject
  public ScheduleNotificationHelper() {}

  public void ensureChannel(Context context) {
    NotificationChannel channel =
        new NotificationChannel(
            ScheduleContracts.CHANNEL_SCHEDULE,
            context.getString(R.string.notification_channel_schedule_name),
            NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription(context.getString(R.string.notification_channel_schedule_desc));
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.createNotificationChannel(channel);
    }
  }

  public boolean notifyListCreated(Context context, ShoppingList list) {
    if (list == null) {
      return false;
    }
    ensureChannel(context);
    String date = list.getShoppingDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra(OrganizingContracts.EXTRA_LIST_ID, list.getId());
    int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    PendingIntent pendingIntent =
        PendingIntent.getActivity(context, (list.getId().hashCode() ^ 0x51), intent, flags);
    Notification notification =
        new NotificationCompat.Builder(context, ScheduleContracts.CHANNEL_SCHEDULE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_schedule_created_title))
            .setContentText(context.getString(R.string.notification_schedule_created_body, date))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();
    try {
      NotificationManagerCompat.from(context)
          .notify(0x53000000 ^ (list.getId().hashCode() & 0x00FFFFFF), notification);
      return true;
    } catch (SecurityException ignored) {
      return false;
    }
  }
}
