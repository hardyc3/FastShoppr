/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class CalendarUpcomingAdapter
    extends ListAdapter<ShoppingListSummary, CalendarUpcomingAdapter.Holder> {

  static final DiffUtil.ItemCallback<ShoppingListSummary> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull ShoppingListSummary oldItem, @NonNull ShoppingListSummary newItem) {
          return Objects.equals(oldItem.getList().getId(), newItem.getList().getId());
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull ShoppingListSummary oldItem, @NonNull ShoppingListSummary newItem) {
          return oldItem.getItemCount() == newItem.getItemCount()
              && oldItem.getStoreCount() == newItem.getStoreCount()
              && Objects.equals(oldItem.getList().getName(), newItem.getList().getName())
              && Objects.equals(
                  oldItem.getList().getShoppingDate(), newItem.getList().getShoppingDate());
        }
      };

  public interface Listener {
    void onOpen(ShoppingListSummary summary);
  }

  private final Listener listener;

  public CalendarUpcomingAdapter(Listener listener) {
    super(DIFF_CALLBACK);
    this.listener = listener;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_calendar_upcoming, parent, false);
    return new Holder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    ShoppingListSummary s = getItem(position);
    h.title.setText(
        h.itemView
            .getContext()
            .getString(
                R.string.calendar_upcoming_row_title,
                s.getList().getName(),
                s.getList().getShoppingDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    h.subtitle.setText(
        h.itemView
            .getContext()
            .getString(
                R.string.list_row_counts,
                h.itemView
                    .getResources()
                    .getQuantityString(R.plurals.item_count, s.getItemCount(), s.getItemCount()),
                h.itemView
                    .getResources()
                    .getQuantityString(
                        R.plurals.store_count, s.getStoreCount(), s.getStoreCount())));
    h.itemView.setOnClickListener(v -> listener.onOpen(s));
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final TextView title;
    final TextView subtitle;

    Holder(@NonNull View itemView) {
      super(itemView);
      title = itemView.findViewById(R.id.textTitle);
      subtitle = itemView.findViewById(R.id.textSubtitle);
    }
  }
}
