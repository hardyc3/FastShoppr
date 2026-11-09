/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import com.fastshoppr.app.data.remote.ink.MlKitInkRecognitionRepository;
import com.fastshoppr.app.data.remote.text.MlKitImageTextRecognitionRepository;
import com.fastshoppr.app.domain.repository.ImageTextRecognitionRepository;
import com.fastshoppr.app.domain.repository.InkRecognitionRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RecognitionModule {

  @Binds
  abstract InkRecognitionRepository bindInkRecognitionRepository(
      MlKitInkRecognitionRepository implementation);

  @Binds
  abstract ImageTextRecognitionRepository bindImageTextRecognitionRepository(
      MlKitImageTextRecognitionRepository implementation);
}
