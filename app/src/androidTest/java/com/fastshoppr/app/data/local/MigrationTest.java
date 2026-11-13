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
