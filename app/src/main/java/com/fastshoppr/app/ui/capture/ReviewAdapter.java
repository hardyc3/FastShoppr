/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.ItemReviewLineBinding;
import java.util.List;
import java.util.Locale;

/** Renders editable review lines and forwards row actions to the Fragment. */
final class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

  interface Listener {
    void onLineSelected(int position, boolean selected);

    void onLineTextChanged(int position, String text);

    void onStoreClicked(int position);

    void onDateClicked(int position);

    String storeLabel(@Nullable String storeId);

    String dateLabel(@Nullable String targetDate);

    boolean isLowConfidence(ReviewViewModel.ReviewLine line);
  }

  private final Listener listener;
  private List<ReviewViewModel.ReviewLine> lines = List.of();

  ReviewAdapter(Listener listener) {
    this.listener = listener;
  }

  void submitLines(List<ReviewViewModel.ReviewLine> updatedLines) {
    lines = List.copyOf(updatedLines);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemReviewLineBinding binding =
        ItemReviewLineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(lines.get(position));
  }

  @Override
  public int getItemCount() {
    return lines.size();
  }

  final class ViewHolder extends RecyclerView.ViewHolder {

    final ItemReviewLineBinding binding;
    final android.widget.CheckBox checkBox;
    final android.widget.EditText textField;
    private final android.widget.TextView displayNameText;
    private final android.widget.TextView confidenceText;
    private final com.google.android.material.button.MaterialButton storeButton;
    private final com.google.android.material.button.MaterialButton dateButton;
    @Nullable private TextWatcher watcher;

    private ViewHolder(ItemReviewLineBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      checkBox = binding.checkBox;
      textField = binding.textField;
      displayNameText = binding.displayNameText;
      confidenceText = binding.confidenceText;
      storeButton = binding.storeButton;
      dateButton = binding.dateButton;
      checkBox.setOnCheckedChangeListener(
          (button, checked) -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              listener.onLineSelected(position, checked);
            }
          });
      storeButton.setOnClickListener(
          v -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              listener.onStoreClicked(position);
            }
          });
      dateButton.setOnClickListener(
          v -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              listener.onDateClicked(position);
            }
          });
    }

    private void bind(ReviewViewModel.ReviewLine line) {
      checkBox.setOnCheckedChangeListener(null);
      checkBox.setChecked(line.isSelected());
      checkBox.setOnCheckedChangeListener(
          (button, checked) -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              listener.onLineSelected(position, checked);
            }
          });
      bindText(line);
      bindConfidence(line);
      bindDisplayName(line);

      storeButton.setText(listener.storeLabel(line.getStoreId()));
      dateButton.setText(listener.dateLabel(line.getTargetDate()));
    }

    private void bindText(ReviewViewModel.ReviewLine line) {
      if (watcher != null) {
        textField.removeTextChangedListener(watcher);
      }
      textField.setText(line.getText());
      watcher =
          new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
              int position = getBindingAdapterPosition();
              if (position != RecyclerView.NO_POSITION) {
                listener.onLineTextChanged(position, editable.toString());
              }
            }
          };
      textField.addTextChangedListener(watcher);
    }

    private void bindDisplayName(ReviewViewModel.ReviewLine line) {
      String displayName = line.getDisplayName();
      if (displayName == null || displayName.isBlank()) {
        displayNameText.setVisibility(View.GONE);
        return;
      }
      displayNameText.setText(
          displayNameText.getContext().getString(R.string.ai_cleanup_accepted_name, displayName));
      displayNameText.setVisibility(View.VISIBLE);
    }

    private void bindConfidence(ReviewViewModel.ReviewLine line) {
      if (line.isManual()) {
        confidenceText.setVisibility(View.GONE);
        return;
      }
      confidenceText.setVisibility(View.VISIBLE);
      if (listener.isLowConfidence(line)) {
        confidenceText.setText(R.string.low_confidence);
        confidenceText.setTextColor(
            resolveColor(textField.getContext(), androidx.appcompat.R.attr.colorError));
      } else {
        confidenceText.setTextColor(
            resolveColor(
                textField.getContext(), com.google.android.material.R.attr.colorOnSurface));
        confidenceText.setText(
            String.format(Locale.getDefault(), "%.0f%%", line.getConfidence() * 100f));
      }
    }

    private int resolveColor(Context context, int attr) {
      TypedValue value = new TypedValue();
      if (context.getTheme().resolveAttribute(attr, value, true) && value.resourceId != 0) {
        return context.getColor(value.resourceId);
      }
      return value.data;
    }
  }
}
