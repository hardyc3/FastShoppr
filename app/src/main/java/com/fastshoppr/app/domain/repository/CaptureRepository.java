/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.CapturedLine;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.RecognizedLine;
import com.fastshoppr.app.domain.observation.Observable;
import java.util.List;

public interface CaptureRepository {

  Observable<List<CaptureSession>> observeByList(String listId);

  List<CaptureSession> getByList(String listId);

  List<CaptureSession> getByListIncludingDeleted(String listId);

  CaptureSession getById(String id);

  void insert(CaptureSession session);

  void complete(String id, long completedAt);

  /**
   * Atomically inserts reviewed items and completes the capture. Rolls back on any failure so no
   * partial item set is committed.
   */
  void saveReviewed(String captureId, List<ListItem> items, long completedAt);

  void softDelete(String id);

  int purgeDeletedBefore(long cutoffMillis);

  /** Serializes recognition output for {@code recognizedLinesJson}. */
  String encodeRecognizedLines(List<RecognizedLine> lines);

  /** Parses {@code recognizedLinesJson}; malformed input yields an empty list. */
  List<RecognizedLine> decodeRecognizedLines(String json);

  /** Serializes the current review draft for {@code recognizedLinesJson}. */
  String encodeDraft(List<CapturedLine> lines);

  /** Parses a review draft; malformed input yields an empty list. */
  List<CapturedLine> decodeDraft(String json);

  /** Persists the current review draft on a capture session. */
  void updateDraft(String captureId, String draftJson, Long completedAt);
}
