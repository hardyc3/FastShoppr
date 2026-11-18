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
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.fastshoppr.app.MainActivity;
import com.fastshoppr.app.R;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class NotificationHelper {

  private final Context context;

  @Inject
  public NotificationHelper(@ApplicationContext Context context) {
    this.context = context;
  }

  public void ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(
            OrganizingContracts.CHANNEL_ORGANIZING,
            context.getString(R.string.notification_channel_organizing_name),
            NotificationManager.IMPORTANCE_LOW);
    channel.setDescription(context.getString(R.string.notification_channel_organizing_desc));
    channel.setShowBadge(false);
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.createNotificationChannel(channel);
    }
  }

  public int notificationId(String listId) {
    if (listId == null) {
      return 0x4F5247;
    }
    int h = listId.hashCode();
    return 0x4F520000 ^ (h & 0x0000FFFF);
  }

  public Notification buildProgress(
      String listId, String title, int done, int total, boolean indeterminate, boolean withCancel) {
    ensureChannel();
    String text =
        total > 0
            ? context.getString(R.string.notification_organizing_progress, Math.max(0, done), total)
            : context.getString(R.string.notification_organizing_title);
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, OrganizingContracts.CHANNEL_ORGANIZING)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent(listId))
            .setProgress(Math.max(total, 0), Math.max(done, 0), indeterminate || total <= 0);
    if (withCancel) {
      builder.addAction(0, context.getString(R.string.notification_cancel), cancelIntent(listId));
    }
    return builder.build();
  }

  public void notifyProgress(
      String listId, String title, int done, int total, boolean indeterminate) {
    try {
      NotificationManagerCompat.from(context)
          .notify(
              notificationId(listId),
              buildProgress(listId, title, done, total, indeterminate, true));
    } catch (SecurityException ignored) {
      // POST_NOTIFICATIONS denied; caller must degrade gracefully
    }
  }

  public void notifyCompletion(String listId, String summary) {
    ensureChannel();
    Notification notification =
        new NotificationCompat.Builder(context, OrganizingContracts.CHANNEL_ORGANIZING)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_organizing_done_title))
            .setContentText(context.getString(R.string.notification_organizing_done_body, summary))
            .setStyle(
                new NotificationCompat.BigTextStyle()
                    .bigText(
                        context.getString(R.string.notification_organizing_done_body, summary)))
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(listId))
            .build();
    try {
      NotificationManagerCompat.from(context).notify(notificationId(listId), notification);
    } catch (SecurityException ignored) {
      // permission denied
    }
  }

  public void cancel(String listId) {
    NotificationManagerCompat.from(context).cancel(notificationId(listId));
  }

  public void cancelAll() {
    NotificationManagerCompat.from(context).cancelAll();
  }

  private PendingIntent contentIntent(String listId) {
    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    if (listId != null) {
      intent.putExtra(OrganizingContracts.EXTRA_LIST_ID, listId);
    }
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return PendingIntent.getActivity(context, notificationId(listId), intent, flags);
  }

  private PendingIntent cancelIntent(String listId) {
    Intent intent = new Intent(context, OrganizingActionReceiver.class);
    intent.setAction(OrganizingContracts.ACTION_CANCEL);
    intent.putExtra(OrganizingContracts.EXTRA_LIST_ID, listId);
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return PendingIntent.getBroadcast(context, notificationId(listId) ^ 0xA5, intent, flags);
  }
}
