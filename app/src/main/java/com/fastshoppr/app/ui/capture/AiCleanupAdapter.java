/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.ItemAiSuggestionBinding;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import java.util.Locale;

/** Renders before/after AI cleanup suggestions with accept, edit, and dismiss actions. */
final class AiCleanupAdapter
    extends ListAdapter<ReviewViewModel.CleanupSuggestion, AiCleanupAdapter.ViewHolder> {

  private static final DiffUtil.ItemCallback<ReviewViewModel.CleanupSuggestion> DIFF =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull ReviewViewModel.CleanupSuggestion oldItem,
            @NonNull ReviewViewModel.CleanupSuggestion newItem) {
          return oldItem.getLineIndex() == newItem.getLineIndex()
              && oldItem.getRawText().equals(newItem.getRawText());
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull ReviewViewModel.CleanupSuggestion oldItem,
            @NonNull ReviewViewModel.CleanupSuggestion newItem) {
          return oldItem.getSuggestedName().equals(newItem.getSuggestedName())
              && oldItem.getConfidence() == newItem.getConfidence()
              && oldItem.getSource().equals(newItem.getSource());
        }
      };

  interface Listener {
    void onAccept(ReviewViewModel.CleanupSuggestion suggestion);

    void onEdit(ReviewViewModel.CleanupSuggestion suggestion);

    void onDismiss(ReviewViewModel.CleanupSuggestion suggestion);
  }

  private final Listener listener;

  AiCleanupAdapter(Listener listener) {
    super(DIFF);
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        ItemAiSuggestionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  final class ViewHolder extends RecyclerView.ViewHolder {

    private final ItemAiSuggestionBinding binding;

    private ViewHolder(ItemAiSuggestionBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      binding.btnAccept.setOnClickListener(v -> withSuggestion(listener::onAccept));
      binding.btnEdit.setOnClickListener(v -> withSuggestion(listener::onEdit));
      binding.btnDismiss.setOnClickListener(v -> withSuggestion(listener::onDismiss));
    }

    private void withSuggestion(
        java.util.function.Consumer<ReviewViewModel.CleanupSuggestion> action) {
      int position = getBindingAdapterPosition();
      if (position != RecyclerView.NO_POSITION) {
        action.accept(getItem(position));
      }
    }

    private void bind(ReviewViewModel.CleanupSuggestion suggestion) {
      binding.rawText.setText(
          binding
              .getRoot()
              .getContext()
              .getString(R.string.ai_cleanup_before, suggestion.getRawText()));
      binding.suggestedText.setText(
          binding
              .getRoot()
              .getContext()
              .getString(R.string.ai_cleanup_after, suggestion.getSuggestedName()));
      String source =
          suggestion.getSource() == AiCleanupSource.ON_DEVICE_AI
              ? binding.getRoot().getContext().getString(R.string.ai_cleanup_source_ai)
              : binding.getRoot().getContext().getString(R.string.ai_cleanup_source_rules);
      String confidence =
          String.format(Locale.getDefault(), "%.0f%%", suggestion.getConfidence() * 100f);
      binding.metaText.setText(
          binding.getRoot().getContext().getString(R.string.ai_cleanup_meta, source, confidence));
    }
  }
}
