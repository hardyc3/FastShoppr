/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class Migrations {

  public static final Migration MIGRATION_1_2 =
      new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL("DROP TABLE IF EXISTS `shell_placeholder`");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `shopping_schedules` ("
                  + "`id` TEXT NOT NULL, "
                  + "`intervalValue` INTEGER NOT NULL, "
                  + "`intervalUnit` TEXT NOT NULL, "
                  + "`nextShoppingDate` TEXT NOT NULL, "
                  + "`isEnabled` INTEGER NOT NULL, "
                  + "`notifyOnCreate` INTEGER NOT NULL, "
                  + "`lastCreatedDate` TEXT, "
                  + "`createdAt` INTEGER NOT NULL, "
                  + "`updatedAt` INTEGER NOT NULL, "
                  + "PRIMARY KEY(`id`))");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `stores` ("
                  + "`id` TEXT NOT NULL, "
                  + "`brand` TEXT NOT NULL, "
                  + "`name` TEXT NOT NULL, "
                  + "`addressLine` TEXT, "
                  + "`city` TEXT, "
                  + "`state` TEXT, "
                  + "`postalCode` TEXT, "
                  + "`latitude` REAL, "
                  + "`longitude` REAL, "
                  + "`isCustom` INTEGER NOT NULL, "
                  + "`isDefault` INTEGER NOT NULL, "
                  + "`isArchived` INTEGER NOT NULL, "
                  + "`hoursJson` TEXT, "
                  + "`createdAt` INTEGER NOT NULL, "
                  + "`updatedAt` INTEGER NOT NULL, "
                  + "PRIMARY KEY(`id`))");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `shopping_lists` ("
                  + "`id` TEXT NOT NULL, "
                  + "`name` TEXT NOT NULL, "
                  + "`shoppingDate` TEXT NOT NULL, "
                  + "`scheduleId` TEXT, "
                  + "`isAutoCreated` INTEGER NOT NULL, "
                  + "`isArchived` INTEGER NOT NULL, "
                  + "`deletedAt` INTEGER, "
                  + "`createdAt` INTEGER NOT NULL, "
                  + "`updatedAt` INTEGER NOT NULL, "
                  + "PRIMARY KEY(`id`), "
                  + "FOREIGN KEY(`scheduleId`) REFERENCES `shopping_schedules`(`id`) "
                  + "ON UPDATE NO ACTION ON DELETE SET NULL )");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `list_items` ("
                  + "`id` TEXT NOT NULL, "
                  + "`listId` TEXT NOT NULL, "
                  + "`storeId` TEXT NOT NULL, "
                  + "`rawText` TEXT NOT NULL, "
                  + "`displayName` TEXT NOT NULL, "
                  + "`normalizedText` TEXT NOT NULL, "
                  + "`quantity` TEXT, "
                  + "`targetDate` TEXT NOT NULL, "
                  + "`aisle` TEXT, "
                  + "`bay` TEXT, "
                  + "`matchedProductName` TEXT, "
                  + "`matchedProductUrl` TEXT, "
                  + "`matchConfidence` REAL, "
                  + "`lookupStatus` TEXT NOT NULL, "
                  + "`lookupSource` TEXT, "
                  + "`lookupError` TEXT, "
                  + "`source` TEXT NOT NULL, "
                  + "`isChecked` INTEGER NOT NULL, "
                  + "`sortPosition` INTEGER NOT NULL, "
                  + "`deletedAt` INTEGER, "
                  + "`createdAt` INTEGER NOT NULL, "
                  + "`updatedAt` INTEGER NOT NULL, "
                  + "PRIMARY KEY(`id`), "
                  + "FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) "
                  + "ON UPDATE NO ACTION ON DELETE CASCADE , "
                  + "FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) "
                  + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `capture_sessions` ("
                  + "`id` TEXT NOT NULL, "
                  + "`listId` TEXT NOT NULL, "
                  + "`source` TEXT NOT NULL, "
                  + "`imageUri` TEXT, "
                  + "`recognizedLinesJson` TEXT NOT NULL, "
                  + "`createdAt` INTEGER NOT NULL, "
                  + "`completedAt` INTEGER, "
                  + "`deletedAt` INTEGER, "
                  + "PRIMARY KEY(`id`), "
                  + "FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) "
                  + "ON UPDATE NO ACTION ON DELETE CASCADE )");

          database.execSQL(
              "CREATE TABLE IF NOT EXISTS `aisle_cache` ("
                  + "`id` TEXT NOT NULL, "
                  + "`storeId` TEXT NOT NULL, "
                  + "`normalizedQuery` TEXT NOT NULL, "
                  + "`aisle` TEXT, "
                  + "`bay` TEXT, "
                  + "`matchedProductName` TEXT, "
                  + "`matchedProductUrl` TEXT, "
                  + "`confidence` REAL, "
                  + "`source` TEXT NOT NULL, "
                  + "`fetchedAt` INTEGER NOT NULL, "
                  + "`expiresAt` INTEGER NOT NULL, "
                  + "PRIMARY KEY(`id`), "
                  + "FOREIGN KEY(`storeId`) REFERENCES `stores`(`id`) "
                  + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

          database.execSQL("CREATE INDEX IF NOT EXISTS `index_stores_brand` ON `stores` (`brand`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_shopping_lists_shoppingDate` "
                  + "ON `shopping_lists` (`shoppingDate`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_shopping_lists_scheduleId` "
                  + "ON `shopping_lists` (`scheduleId`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_shopping_lists_isArchived` "
                  + "ON `shopping_lists` (`isArchived`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_shopping_lists_deletedAt` "
                  + "ON `shopping_lists` (`deletedAt`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_listId` "
                  + "ON `list_items` (`listId`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_storeId` "
                  + "ON `list_items` (`storeId`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_targetDate` "
                  + "ON `list_items` (`targetDate`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_lookupStatus` "
                  + "ON `list_items` (`lookupStatus`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_deletedAt` "
                  + "ON `list_items` (`deletedAt`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_capture_sessions_listId` "
                  + "ON `capture_sessions` (`listId`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_capture_sessions_deletedAt` "
                  + "ON `capture_sessions` (`deletedAt`)");
          database.execSQL(
              "CREATE UNIQUE INDEX IF NOT EXISTS "
                  + "`index_aisle_cache_storeId_normalizedQuery` "
                  + "ON `aisle_cache` (`storeId`, `normalizedQuery`)");
        }
      };

  public static final Migration MIGRATION_2_3 =
      new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL("ALTER TABLE `list_items` ADD COLUMN `matchCandidatesJson` TEXT");
        }
      };

  public static final Migration MIGRATION_3_4 =
      new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_list_deleted_sort` "
                  + "ON `list_items` (`listId`, `deletedAt`, `sortPosition`)");
          database.execSQL(
              "CREATE INDEX IF NOT EXISTS `index_list_items_list_deleted_store_sort` "
                  + "ON `list_items` (`listId`, `deletedAt`, `storeId`, `sortPosition`)");
        }
      };

  private Migrations() {}
}
