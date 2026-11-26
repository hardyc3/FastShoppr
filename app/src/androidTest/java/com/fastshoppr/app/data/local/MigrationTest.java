/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MigrationTest {

  private static final String TEST_DATABASE = "migration-test";

  @Rule
  public final MigrationTestHelper helper =
      new MigrationTestHelper(
          InstrumentationRegistry.getInstrumentation(),
          FastShopprDatabase.class,
          Collections.emptyList(),
          new FrameworkSQLiteOpenHelperFactory());

  @Test
  public void migratesVersionOneToVersionTwo() throws IOException {
    SupportSQLiteDatabase versionOne = helper.createDatabase(TEST_DATABASE, 1);
    versionOne.execSQL("INSERT INTO shell_placeholder (id) VALUES ('placeholder')");
    versionOne.close();

    SupportSQLiteDatabase versionTwo =
        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, Migrations.MIGRATION_1_2);

    assertFalse(tableExists(versionTwo, "shell_placeholder"));
    assertTrue(tableExists(versionTwo, "stores"));
    assertTrue(tableExists(versionTwo, "shopping_lists"));
    assertTrue(tableExists(versionTwo, "list_items"));
    assertTrue(tableExists(versionTwo, "capture_sessions"));
    assertTrue(tableExists(versionTwo, "shopping_schedules"));
    assertTrue(tableExists(versionTwo, "aisle_cache"));
    assertEquals(0, rowCount(versionTwo, "stores"));
    versionTwo.close();
  }

  @Test
  public void migratesVersionTwoToVersionThree() throws IOException, SQLiteException {
    SupportSQLiteDatabase versionTwo = helper.createDatabase(TEST_DATABASE, 2);
    versionTwo.execSQL(
        "INSERT INTO list_items (id, listId, storeId, rawText, displayName, normalizedText, "
            + "targetDate, lookupStatus, source, isChecked, sortPosition, createdAt, updatedAt) "
            + "VALUES ('item-1', 'list-1', 'store-1', 'milk', 'milk', 'milk', '2026-11-13', "
            + "'PENDING', 'TYPED', 0, 0, 1, 1)");
    versionTwo.close();

    SupportSQLiteDatabase versionThree =
        helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true, Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3);

    Cursor cursor = versionThree.query("SELECT matchCandidatesJson FROM list_items");
    try {
      assertTrue(cursor.moveToFirst());
      assertEquals(1, cursor.getCount());
      assertTrue(cursor.isNull(0));
    } finally {
      cursor.close();
    }
    versionThree.close();
  }

  @Test
  public void migratesVersionThreeToVersionFourWithListDetailIndexes() throws IOException {
    SupportSQLiteDatabase versionThree = helper.createDatabase(TEST_DATABASE, 3);
    versionThree.execSQL(
        "INSERT INTO list_items (id, listId, storeId, rawText, displayName, normalizedText, "
            + "targetDate, lookupStatus, source, isChecked, sortPosition, createdAt, updatedAt) "
            + "VALUES ('item-1', 'list-1', 'store-1', 'milk', 'milk', 'milk', '2026-11-13', "
            + "'PENDING', 'TYPED', 0, 0, 1, 1)");
    versionThree.close();

    SupportSQLiteDatabase versionFour =
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4);

    assertTrue(indexExists(versionFour, "index_list_items_list_deleted_sort"));
    assertTrue(indexExists(versionFour, "index_list_items_list_deleted_store_sort"));
    assertEquals(1, rowCount(versionFour, "list_items"));
    versionFour.close();
  }

  @Test
  public void migratesVersionFourToVersionFiveAndRetiresRemovedStore() throws IOException {
    SupportSQLiteDatabase versionFour = helper.createDatabase(TEST_DATABASE, 4);
    versionFour.execSQL(
        "INSERT INTO stores (id, brand, name, isCustom, isDefault, isArchived, createdAt, updatedAt) "
            + "VALUES ('d6d7e4f5-6a7b-4c8d-9e0f-1a2b3c4d5e6f', 'MACYS', 'Legacy Store', 0, 0, 0, 1, 1)");
    versionFour.execSQL(
        "INSERT INTO aisle_cache (id, storeId, normalizedQuery, source, fetchedAt, expiresAt) "
            + "VALUES ('cache-1', 'd6d7e4f5-6a7b-4c8d-9e0f-1a2b3c4d5e6f', 'milk', 'LOCAL', 1, 1)");
    versionFour.close();

    SupportSQLiteDatabase versionFive =
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5);

    Cursor cursor =
        versionFive.query(
            "SELECT brand, name, isCustom, isDefault, isArchived FROM stores WHERE id = ?",
            new Object[] {"d6d7e4f5-6a7b-4c8d-9e0f-1a2b3c4d5e6f"});
    try {
      assertTrue(cursor.moveToFirst());
      assertEquals("CUSTOM", cursor.getString(0));
      assertEquals("Removed store", cursor.getString(1));
      assertEquals(1, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(1, cursor.getInt(4));
    } finally {
      cursor.close();
    }
    assertEquals(0, rowCount(versionFive, "aisle_cache"));
    versionFive.close();
  }

  @Test
  public void migratesVersionFiveToVersionSixAndDropsStoreHours() throws IOException {
    SupportSQLiteDatabase versionFive = helper.createDatabase(TEST_DATABASE, 5);
    versionFive.execSQL(
        "INSERT INTO stores (id, brand, name, hoursJson, isCustom, isDefault, isArchived, "
            + "createdAt, updatedAt) VALUES ('store-1', 'WALMART', 'Walmart', "
            + "'Mon-Fri 8-10', 0, 0, 0, 1, 1)");
    assertTrue(columnExists(versionFive, "stores", "hoursJson"));
    versionFive.close();

    SupportSQLiteDatabase versionSix =
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6);

    assertFalse(columnExists(versionSix, "stores", "hoursJson"));
    assertEquals(1, rowCount(versionSix, "stores"));
    versionSix.close();
  }

  @Test
  public void migratesVersionSixToVersionSevenAndRetiresTargetStore() throws IOException {
    SupportSQLiteDatabase versionSix = helper.createDatabase(TEST_DATABASE, 6);
    versionSix.execSQL(
        "INSERT INTO stores (id, brand, name, isCustom, isDefault, isArchived, createdAt, updatedAt) "
            + "VALUES ('b4b5e2c3-4d5e-4f6a-9b0c-1d2e3f4a5b6c', 'TARGET', 'Target Store', "
            + "0, 1, 0, 1, 1)");
    versionSix.execSQL(
        "INSERT INTO aisle_cache (id, storeId, normalizedQuery, source, fetchedAt, expiresAt) "
            + "VALUES ('cache-6', 'b4b5e2c3-4d5e-4f6a-9b0c-1d2e3f4a5b6c', 'milk', 'LOCAL', 1, 1)");
    versionSix.close();

    SupportSQLiteDatabase versionSeven =
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6,
            Migrations.MIGRATION_6_7);

    Cursor cursor =
        versionSeven.query(
            "SELECT brand, name, isCustom, isDefault, isArchived FROM stores WHERE id = ?",
            new Object[] {"b4b5e2c3-4d5e-4f6a-9b0c-1d2e3f4a5b6c"});
    try {
      assertTrue(cursor.moveToFirst());
      assertEquals("CUSTOM", cursor.getString(0));
      assertEquals("Removed store", cursor.getString(1));
      assertEquals(1, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(1, cursor.getInt(4));
    } finally {
      cursor.close();
    }
    assertEquals(0, rowCount(versionSeven, "aisle_cache"));
    versionSeven.close();
  }

  private static boolean columnExists(SupportSQLiteDatabase database, String table, String column) {
    Cursor cursor = database.query("PRAGMA table_info(`" + table + "`)");
    try {
      while (cursor.moveToNext()) {
        if (column.equals(cursor.getString(1))) {
          return true;
        }
      }
      return false;
    } finally {
      cursor.close();
    }
  }

  private static boolean indexExists(SupportSQLiteDatabase database, String index) {
    Cursor cursor =
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
            new Object[] {index});
    try {
      return cursor.moveToFirst();
    } finally {
      cursor.close();
    }
  }

  private static boolean tableExists(SupportSQLiteDatabase database, String table) {
    Cursor cursor =
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            new Object[] {table});
    try {
      return cursor.moveToFirst();
    } finally {
      cursor.close();
    }
  }

  private static int rowCount(SupportSQLiteDatabase database, String table) {
    Cursor cursor = database.query("SELECT COUNT(*) FROM " + table);
    try {
      return cursor.moveToFirst() ? cursor.getInt(0) : -1;
    } finally {
      cursor.close();
    }
  }
}
