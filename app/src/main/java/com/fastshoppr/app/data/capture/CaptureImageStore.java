/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.capture;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Owns capture images in app-private cache. Files live under {@code cache/captures} and are deleted
 * after the review screen is saved or discarded.
 */
@Singleton
public final class CaptureImageStore {

  static final String DIRECTORY_NAME = "captures";

  @Nullable private final Context context;
  private final File capturesDir;

  @Inject
  public CaptureImageStore(@ApplicationContext Context context) {
    this(context, new File(context.getCacheDir(), DIRECTORY_NAME));
  }

  CaptureImageStore(@Nullable Context context, File capturesDir) {
    this.context = context;
    this.capturesDir = capturesDir;
  }

  /** Creates an empty JPEG file for CameraX output. */
  public File newImageFile() throws IOException {
    ensureDirectory();
    return File.createTempFile("capture_", ".jpg", capturesDir);
  }

  /** Copies a picked image into app-private cache and returns the cached copy. */
  public File copyToCache(Uri source) throws IOException {
    if (source == null) {
      throw new IOException("source uri must not be null");
    }
    if (context == null) {
      throw new IOException("context is required to copy images");
    }
    ensureDirectory();
    File target = File.createTempFile("capture_", ".jpg", capturesDir);
    try (InputStream in = context.getContentResolver().openInputStream(source);
        OutputStream out = new FileOutputStream(target)) {
      if (in == null) {
        throw new IOException("could not open source uri");
      }
      byte[] buffer = new byte[16 * 1024];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    } catch (IOException exception) {
      //noinspection ResultOfMethodCallIgnored
      target.delete();
      throw exception;
    }
    return target;
  }

  /** Deletes a cached capture image. Paths outside the captures directory are ignored. */
  public void delete(@Nullable String path) {
    if (path == null || path.isBlank()) {
      return;
    }
    try {
      File file = new File(path).getCanonicalFile();
      File dir = capturesDir.getCanonicalFile();
      if (!file.getPath().startsWith(dir.getPath() + File.separator)) {
        return;
      }
      if (file.exists()) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
      }
    } catch (IOException exception) {
      return;
    }
  }

  /** Deletes every cached capture image. Used by the "Delete all data" action. */
  public void deleteAll() {
    File[] files = capturesDir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isFile()) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
      }
    }
  }

  public boolean exists(@Nullable String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    return new File(path).exists();
  }

  private void ensureDirectory() {
    if (!capturesDir.exists() && !capturesDir.mkdirs()) {
      throw new IllegalStateException("could not create captures directory");
    }
  }
}
