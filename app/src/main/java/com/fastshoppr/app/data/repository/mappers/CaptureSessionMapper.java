/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository.mappers;

import com.fastshoppr.app.data.local.entity.CaptureSessionEntity;
import com.fastshoppr.app.domain.model.CaptureSession;

public final class CaptureSessionMapper {

  private CaptureSessionMapper() {}

  public static CaptureSessionEntity toEntity(CaptureSession session) {
    return new CaptureSessionEntity(
        session.getId(),
        session.getListId(),
        session.getSource(),
        session.getImageUri(),
        session.getRecognizedLinesJson(),
        session.getCreatedAt(),
        session.getCompletedAt(),
        session.getDeletedAt());
  }

  public static CaptureSession toDomain(CaptureSessionEntity entity) {
    return new CaptureSession(
        entity.id,
        entity.listId,
        entity.source,
        entity.imageUri,
        entity.recognizedLinesJson,
        entity.createdAt,
        entity.completedAt,
        entity.deletedAt);
  }
}
