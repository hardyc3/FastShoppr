/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

  private static final String USER_AGENT = "FastShoppr/1.0 (Android; aisle lookup)";

  private NetworkModule() {}

  @Provides
  @Singleton
  static OkHttpClient provideOkHttpClient() {
    return new OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor(chain -> chain.proceed(withDefaultHeaders(chain.request())))
        .build();
  }

  /** Adds shared headers without overwriting a caller-supplied user agent. */
  static Request withDefaultHeaders(Request request) {
    Request.Builder builder = request.newBuilder();
    if (request.header("User-Agent") == null) {
      builder.header("User-Agent", USER_AGENT);
    }
    return builder.header("Accept-Language", "en-US,en;q=0.9").build();
  }

  public static String userAgent() {
    return USER_AGENT;
  }
}
