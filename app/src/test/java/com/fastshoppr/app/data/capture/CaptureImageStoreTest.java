/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class CaptureImageStoreTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File capturesDir;

  @Before
  public void setUp() throws Exception {
    capturesDir = temporaryFolder.newFolder(CaptureImageStore.DIRECTORY_NAME);
  }

  @Test
  public void newImageFileCreatesFileInsideCapturesDirectory() throws Exception {
    CaptureImageStore store = new CaptureImageStore(null, capturesDir);

    File file = store.newImageFile();

    assertTrue(file.exists());
    assertEquals(capturesDir.getCanonicalFile(), file.getParentFile().getCanonicalFile());
    assertTrue(file.getName().endsWith(".jpg"));
  }

  @Test
  public void deleteRemovesCachedImage() throws Exception {
    CaptureImageStore store = new CaptureImageStore(null, capturesDir);
    File file = store.newImageFile();

    store.delete(file.getAbsolutePath());

    assertFalse(file.exists());
    assertFalse(store.exists(file.getAbsolutePath()));
  }

  @Test
  public void deleteIgnoresPathsOutsideCapturesDirectory() throws Exception {
    CaptureImageStore store = new CaptureImageStore(null, capturesDir);
    File outside = temporaryFolder.newFile("outside.jpg");

    store.delete(outside.getAbsolutePath());

    assertTrue(outside.exists());
  }

  @Test
  public void deleteIgnoresNullOrEmptyPaths() {
    CaptureImageStore store = new CaptureImageStore(null, capturesDir);

    store.delete(null);
    store.delete("");
    store.delete("   ");
  }

  @Test
  public void copyToCacheCopiesContentIntoCapturesDirectory() throws Exception {
    byte[] payload = "image-bytes".getBytes(StandardCharsets.UTF_8);
    Context context = mock(Context.class);
    ContentResolver resolver = mock(ContentResolver.class);
    Uri source = mock(Uri.class);
    when(context.getContentResolver()).thenReturn(resolver);
    when(resolver.openInputStream(source))
        .thenReturn(new ByteArrayInputStream(payload));
    CaptureImageStore store = new CaptureImageStore(context, capturesDir);

    File copy = store.copyToCache(source);

    assertTrue(copy.exists());
    assertEquals(capturesDir.getCanonicalFile(), copy.getParentFile().getCanonicalFile());
    assertTrue(java.util.Arrays.equals(payload, Files.readAllBytes(copy.toPath())));
  }
}
